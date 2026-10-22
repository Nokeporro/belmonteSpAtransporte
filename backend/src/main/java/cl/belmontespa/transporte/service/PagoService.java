package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.CambiarEstadoPagoDTORequest;
import cl.belmontespa.transporte.dto.request.PagoDTORequest;
import cl.belmontespa.transporte.dto.response.PagoDTOResponse;
import cl.belmontespa.transporte.model.EstadoPago;
import cl.belmontespa.transporte.model.Pago;
import cl.belmontespa.transporte.model.Solicitante;
import cl.belmontespa.transporte.model.Viaje;
import cl.belmontespa.transporte.repository.BancoRepository;
import cl.belmontespa.transporte.repository.EstadoPagoRepository;
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
	static final String RECHAZADO = "RECHAZADO";
	static final String ANULADO = "ANULADO";

	private final PagoRepository pagoRepository;
	private final ViajeRepository viajeRepository;
	private final EstadoPagoRepository estadoPagoRepository;
	private final BancoRepository bancoRepository;
	private final MedioPagoRepository medioPagoRepository;
	private final TipoReciboRepository tipoReciboRepository;
	private final SolicitanteService solicitanteService;
	private final Clock clock;

	public PagoService(PagoRepository pagoRepository, ViajeRepository viajeRepository,
			EstadoPagoRepository estadoPagoRepository, BancoRepository bancoRepository,
			MedioPagoRepository medioPagoRepository, TipoReciboRepository tipoReciboRepository,
			SolicitanteService solicitanteService, Clock clock) {
		this.pagoRepository = pagoRepository;
		this.viajeRepository = viajeRepository;
		this.estadoPagoRepository = estadoPagoRepository;
		this.bancoRepository = bancoRepository;
		this.medioPagoRepository = medioPagoRepository;
		this.tipoReciboRepository = tipoReciboRepository;
		this.solicitanteService = solicitanteService;
		this.clock = clock;
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
		Viaje viaje = viajeRepository.findByFolio(request.folio())
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
		pago.setFechaPago(LocalDateTime.now(clock));
		viaje.setPago(pago);
		return pago;
	}

	@Override
	protected PagoDTOResponse aResponse(Pago pago) {
		return PagoDTOResponse.from(pago);
	}

	// CU-12/RF19: solo un pago Pendiente pasa a Pagado o Rechazado.
	@Transactional
	public PagoDTOResponse cambiarEstado(String hash, CambiarEstadoPagoDTORequest request) {
		Pago pago = buscarOFallar(hash);
		if (!PENDIENTE.equals(pago.getEstadoPago().getEstado())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede cambiar un pago Pendiente");
		}
		pago.setEstadoPago(estado(request.estado().toUpperCase(Locale.ROOT)));
		return aResponse(pago);
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
				.map(viaje -> aResponse(viaje.getPago()))
				.toList();
	}

	// CU-08: al cancelar el viaje el pago pasa a Anulado, salvo que ya sea terminal.
	void anular(Pago pago) {
		String actual = pago.getEstadoPago().getEstado();
		if (!RECHAZADO.equals(actual) && !ANULADO.equals(actual)) {
			pago.setEstadoPago(estado(ANULADO));
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
