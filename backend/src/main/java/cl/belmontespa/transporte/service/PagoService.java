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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

	public PagoService(PagoRepository pagoRepository, ViajeRepository viajeRepository,
			EstadoPagoRepository estadoPagoRepository, BancoRepository bancoRepository,
			MedioPagoRepository medioPagoRepository, TipoReciboRepository tipoReciboRepository,
			SolicitanteService solicitanteService) {
		this.pagoRepository = pagoRepository;
		this.viajeRepository = viajeRepository;
		this.estadoPagoRepository = estadoPagoRepository;
		this.bancoRepository = bancoRepository;
		this.medioPagoRepository = medioPagoRepository;
		this.tipoReciboRepository = tipoReciboRepository;
		this.solicitanteService = solicitanteService;
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
		pago.setFechaPago(LocalDateTime.now());
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

	// RF20: pagos de los viajes del Solicitante.
	@Transactional(readOnly = true)
	public List<PagoDTOResponse> listarDeSolicitante(String correo) {
		Solicitante solicitante = solicitanteService.buscarOFallar(correo);
		return viajeRepository.findBySolicitanteIdUsuarioOrderByFhAgendaDesc(solicitante.getIdUsuario()).stream()
				.map(Viaje::getPago)
				.filter(Objects::nonNull)
				.map(this::aResponse)
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
