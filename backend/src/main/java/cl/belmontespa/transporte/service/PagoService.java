package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.client.FirebaseClient;
import cl.belmontespa.transporte.dto.request.CambiarEstadoPagoDTORequest;
import cl.belmontespa.transporte.dto.request.PagoDTORequest;
import cl.belmontespa.transporte.dto.response.PagoDTOResponse;
import cl.belmontespa.transporte.model.EstadoPago;
import cl.belmontespa.transporte.model.Pago;
import cl.belmontespa.transporte.model.Solicitante;
import cl.belmontespa.transporte.model.Viaje;
import cl.belmontespa.transporte.repository.BancoRepository;
import cl.belmontespa.transporte.repository.EstadoPagoRepository;
import cl.belmontespa.transporte.repository.EstadoSolicitudRepository;
import cl.belmontespa.transporte.repository.MedioPagoRepository;
import cl.belmontespa.transporte.repository.PagoRepository;
import cl.belmontespa.transporte.repository.TipoReciboRepository;
import cl.belmontespa.transporte.repository.ViajeRepository;
import cl.belmontespa.transporte.validation.Reglas;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PagoService extends CrudService<Pago, String, PagoDTORequest, PagoDTOResponse> {

	static final String PENDIENTE = "PENDIENTE";
	static final String PAGADO = "PAGADO";
	static final String RECHAZADO = "RECHAZADO";
	static final String ANULADO = "ANULADO";
	static final String REEMBOLSADO = "REEMBOLSADO";

	private final PagoRepository pagoRepository;
	private final ViajeRepository viajeRepository;
	private final EstadoPagoRepository estadoPagoRepository;
	private final BancoRepository bancoRepository;
	private final MedioPagoRepository medioPagoRepository;
	private final TipoReciboRepository tipoReciboRepository;
	private final SolicitanteService solicitanteService;
	private final Clock clock;
	private final EstadoSolicitudRepository estadoSolicitudRepository;
	private final FirebaseClient firebaseClient;

	public PagoService(PagoRepository pagoRepository, ViajeRepository viajeRepository,
			EstadoPagoRepository estadoPagoRepository, BancoRepository bancoRepository,
			MedioPagoRepository medioPagoRepository, TipoReciboRepository tipoReciboRepository,
			SolicitanteService solicitanteService, Clock clock,
			EstadoSolicitudRepository estadoSolicitudRepository, FirebaseClient firebaseClient) {
		this.pagoRepository = pagoRepository;
		this.viajeRepository = viajeRepository;
		this.estadoPagoRepository = estadoPagoRepository;
		this.bancoRepository = bancoRepository;
		this.medioPagoRepository = medioPagoRepository;
		this.tipoReciboRepository = tipoReciboRepository;
		this.solicitanteService = solicitanteService;
		this.clock = clock;
		this.estadoSolicitudRepository = estadoSolicitudRepository;
		this.firebaseClient = firebaseClient;
	}

	@Override
	protected JpaRepository<Pago, Long> repositorio() {
		return pagoRepository;
	}

	@Override
	protected Optional<Pago> buscar(String hash) {
		return pagoRepository.findByHash(hash);
	}

	// CU-11/RF18: nace Pendiente, lo paga la empresa del Solicitante y queda asociado al viaje.
	@Override
	protected Pago nuevaEntidad(PagoDTORequest request) {
		Viaje viaje = viajeRepository.bloquearPorFolio(request.folio())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El viaje no existe"));
		if (ViajeService.CANCELADO.equals(viaje.getEstado().getNombreEstado())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El viaje está cancelado");
		}
		if (viaje.getPago() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El viaje ya tiene un pago");
		}
		Pago pago = new Pago();
		pago.setHash(UUID.randomUUID().toString().replace("-", ""));
		pago.setMonto(request.monto());
		pago.setEstadoPago(estado(PENDIENTE));
		pago.setBanco(bancoRepository.findByCodSbif(request.codSbif())
				.orElseThrow(() -> catalogoInvalido("banco", request.codSbif())));
		pago.setMedioPago(medioPagoRepository.findByNombre(request.medioPago())
				.orElseThrow(() -> catalogoInvalido("medio de pago", request.medioPago())));
		pago.setTipoRecibo(tipoReciboRepository.findByNombre(request.tipoRecibo())
				.orElseThrow(() -> catalogoInvalido("tipo de recibo", request.tipoRecibo())));
		pago.setEmpresa(viaje.getSolicitante().getEmpresa());
		viaje.setPago(pago);
		return pago;
	}

	@Override
	protected PagoDTOResponse aResponse(Pago pago) {
		String folio = viajeRepository.folioDePago(pago.getId())
				.orElseThrow(() -> new IllegalStateException("El pago no está asociado a un viaje"));
		return PagoDTOResponse.from(pago, folio);
	}

	@Override
	@Transactional
	public PagoDTOResponse crear(PagoDTORequest request) {
		Pago pago = pagoRepository.save(nuevaEntidad(request));
		return PagoDTOResponse.from(pago, request.folio());
	}

	// CU-12/RF19: P2/P3 y V6 se confirman juntos. El bloqueo precede a la lectura del pago
	// para que dos cambios o una cancelación concurrentes no usen un estado anterior.
	@Transactional
	public PagoDTOResponse cambiarEstado(String hash, CambiarEstadoPagoDTORequest request) {
		String claveViaje = viajeRepository.folioPorHashPago(hash)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		Viaje viaje = viajeRepository.bloquearPorFolio(claveViaje)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		// Orden único: VIAJE, luego PAGO. La lectura bloqueante ve el último estado confirmado
		// aunque la búsqueda del folio haya abierto una instantánea REPEATABLE_READ anterior.
		Pago pago = pagoRepository.bloquearPorHash(hash)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		String actual = pago.getEstadoPago().getEstado();
		String nuevo = request.estado().toUpperCase(Locale.ROOT);
		boolean permitido = PENDIENTE.equals(actual) && List.of(PAGADO, RECHAZADO, ANULADO).contains(nuevo)
				|| PAGADO.equals(actual) && ANULADO.equals(nuevo);
		if (!permitido) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede pasar de " + actual + " a " + nuevo);
		}
		pago.setEstadoPago(estado(nuevo));
		if (PAGADO.equals(nuevo)) {
			pago.setFechaPago(LocalDateTime.now(clock).withNano(0));
		}
		String folio = viaje.getFolio();
		String token = viaje.getSolicitante().getUsuario().getTokenFcm();
		Map<String, String> datos = Map.of("tipo", "VIAJE", "folio", folio);
		if (ANULADO.equals(nuevo) && List.of(ViajeService.PENDIENTE, ViajeService.ASIGNADO)
				.contains(viaje.getEstado().getNombreEstado())) {
			viaje.setEstado(estadoSolicitudRepository.findByNombreEstado(ViajeService.CANCELADO)
					.orElseThrow(() -> new IllegalStateException("Falta el estado CANCELADO")));
			firebaseClient.notificar(token, "Viaje cancelado", "Tu viaje " + folio + " fue cancelado", datos);
			if (viaje.getConductor() != null) {
				firebaseClient.notificar(viaje.getConductor().getUsuario().getTokenFcm(),
						"Viaje cancelado", "El viaje " + folio + " fue cancelado", datos);
			}
		}
		String etiqueta = switch (nuevo) {
			case PAGADO -> "Pagado";
			case RECHAZADO -> "Rechazado";
			default -> "Anulado";
		};
		firebaseClient.notificar(token, "Pago del viaje " + folio, "Estado: " + etiqueta, datos);
		return PagoDTOResponse.from(pago, folio);
	}

	// Todos los pagos, por folio descendente. El pago no guarda su folio: es el del viaje que lo
	// referencia (viaje.pago_id), por eso la lista parte de los viajes.
	@Override
	@Transactional(readOnly = true)
	public List<PagoDTOResponse> listar() {
		return pagosPorFolioDescendente(viajeRepository.findByPagoIsNotNull());
	}

	// RF20: pagos de los viajes del Solicitante, por folio descendente.
	@Transactional(readOnly = true)
	public List<PagoDTOResponse> listarDeSolicitante(String correo) {
		Solicitante solicitante = solicitanteService.buscarOFallar(correo);
		return pagosPorFolioDescendente(viajeRepository.findBySolicitanteIdUsuario(solicitante.getIdUsuario()));
	}

	private List<PagoDTOResponse> pagosPorFolioDescendente(List<Viaje> viajes) {
		return viajes.stream()
				.filter(viaje -> viaje.getPago() != null)
				.sorted(Comparator.comparing(Viaje::getFolio, Reglas.FOLIO_POR_NUMERO).reversed())
				.map(viaje -> PagoDTOResponse.from(viaje.getPago(), viaje.getFolio()))
				.toList();
	}

	// V5/P4/P5: el reembolso del 50 % usa pesos enteros y se redondea hacia abajo.
	void cancelarPorSolicitante(Pago pago) {
		String actual = pago.getEstadoPago().getEstado();
		if (PENDIENTE.equals(actual)) {
			pago.setEstadoPago(estado(ANULADO));
		} else if (PAGADO.equals(actual)) {
			pago.setMontoReembolsado(pago.getMonto() / 2);
			pago.setEstadoPago(estado(REEMBOLSADO));
		}
	}

	// V7: el Administrador reembolsa el 100 %; con el viaje vencido y el pago PAGADO fija él el monto, de
	// 0 al total. En cualquier otro caso el `montoReembolso` se ignora.
	int reembolsoDelAdministrador(Pago pago, boolean vencido, Integer fijado) {
		if (!PAGADO.equals(pago.getEstadoPago().getEstado()) || !vencido) {
			return pago.getMonto();
		}
		if (fijado == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montoReembolso: Es obligatorio.");
		}
		if (fijado < 0 || fijado > pago.getMonto()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"montoReembolso: Debe estar entre 0 y " + pago.getMonto() + ".");
		}
		return fijado;
	}

	// V7/P4/P5: un reembolso de 0 deja el pago PAGADO, sin monto reembolsado.
	void cancelarPorAdministrador(Pago pago, int reembolso) {
		String actual = pago.getEstadoPago().getEstado();
		if (PENDIENTE.equals(actual)) {
			pago.setEstadoPago(estado(ANULADO));
		} else if (PAGADO.equals(actual) && reembolso > 0) {
			pago.setMontoReembolsado(reembolso);
			pago.setEstadoPago(estado(REEMBOLSADO));
		}
	}

	private EstadoPago estado(String nombre) {
		return estadoPagoRepository.findByEstado(nombre)
				.orElseThrow(() -> new IllegalStateException("Falta el estado de pago " + nombre));
	}

	private static ResponseStatusException catalogoInvalido(String catalogo, Object valor) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe el " + catalogo + " " + valor);
	}
}
