package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.client.FirebaseClient;
import cl.belmontespa.transporte.dto.request.AsignarViajeDTORequest;
import cl.belmontespa.transporte.dto.request.CambiarEstadoViajeDTORequest;
import cl.belmontespa.transporte.dto.request.CancelarViajeDTORequest;
import cl.belmontespa.transporte.dto.request.RechazarViajeDTORequest;
import cl.belmontespa.transporte.dto.request.ViajeDTORequest;
import cl.belmontespa.transporte.dto.response.ViajeDTOResponse;
import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.DetalleMovilConductorId;
import cl.belmontespa.transporte.model.EstadoSolicitud;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.model.Pago;
import cl.belmontespa.transporte.model.Viaje;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import cl.belmontespa.transporte.repository.DetalleMovilConductorRepository;
import cl.belmontespa.transporte.repository.DetalleViajePasajeroRepository;
import cl.belmontespa.transporte.repository.EstadoSolicitudRepository;
import cl.belmontespa.transporte.repository.ViajeRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ViajeService extends CrudService<Viaje, String, ViajeDTORequest, ViajeDTOResponse> {

	static final String PENDIENTE = "PENDIENTE";
	static final String ASIGNADO = "ASIGNADO";
	static final String EN_CURSO = "EN_CURSO";
	static final String FINALIZADO = "FINALIZADO";
	static final String CANCELADO = "CANCELADO";

	private static final Set<String> AGENDA = Set.of(ASIGNADO, EN_CURSO, FINALIZADO);

	private final ViajeRepository viajeRepository;
	private final EstadoSolicitudRepository estadoSolicitudRepository;
	private final DetalleViajePasajeroRepository detalleViajePasajeroRepository;
	private final DetalleMovilConductorRepository detalleMovilConductorRepository;
	private final AdministradorRepository administradorRepository;
	private final SolicitanteService solicitanteService;
	private final ConductorService conductorService;
	private final MovilService movilService;
	private final PagoService pagoService;
	private final UbicacionService ubicacionService;
	private final FirebaseClient firebaseClient;
	private final EntityManager entityManager;
	private final Clock clock;

	public ViajeService(ViajeRepository viajeRepository, EstadoSolicitudRepository estadoSolicitudRepository,
			DetalleViajePasajeroRepository detalleViajePasajeroRepository,
			DetalleMovilConductorRepository detalleMovilConductorRepository, AdministradorRepository administradorRepository,
			SolicitanteService solicitanteService, ConductorService conductorService, MovilService movilService,
			PagoService pagoService, UbicacionService ubicacionService, FirebaseClient firebaseClient,
			EntityManager entityManager, Clock clock) {
		this.viajeRepository = viajeRepository;
		this.estadoSolicitudRepository = estadoSolicitudRepository;
		this.detalleViajePasajeroRepository = detalleViajePasajeroRepository;
		this.detalleMovilConductorRepository = detalleMovilConductorRepository;
		this.administradorRepository = administradorRepository;
		this.solicitanteService = solicitanteService;
		this.conductorService = conductorService;
		this.movilService = movilService;
		this.pagoService = pagoService;
		this.ubicacionService = ubicacionService;
		this.firebaseClient = firebaseClient;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Override
	protected JpaRepository<Viaje, Long> repositorio() {
		return viajeRepository;
	}

	@Override
	protected Optional<Viaje> buscar(String folio) {
		return viajeRepository.findByFolio(folio);
	}

	@Override
	protected Viaje nuevaEntidad(ViajeDTORequest request) {
		throw new IllegalStateException("Usar ViajeService.crear(request, correoSolicitante)");
	}

	@Override
	protected ViajeDTOResponse aResponse(Viaje viaje) {
		return ViajeDTOResponse.from(viaje, detalleViajePasajeroRepository.findByIdViaje(viaje.getIdViaje()));
	}

	@Transactional
	public ViajeDTOResponse crear(ViajeDTORequest request, String correoSolicitante) {
		Viaje viaje = new Viaje();
		viaje.setSolicitante(solicitanteService.buscarOFallar(correoSolicitante));
		if (!"APROBADO".equals(viaje.getSolicitante().getEstadoSolicitante().getEstado())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta debe estar aprobada");
		}
		LocalDateTime ahora = LocalDateTime.now(clock);
		LocalDateTime agenda = request.fhAgenda().withSecond(0).withNano(0);
		if (agenda.isBefore(ahora.plusHours(2)) || agenda.isAfter(ahora.plusDays(90))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fhAgenda: Debe estar entre 2 horas y 90 días desde ahora.");
		}
		if (request.origen().latitud().compareTo(request.destino().latitud()) == 0
				&& request.origen().longitud().compareTo(request.destino().longitud()) == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destino: Debe ser distinto del origen.");
		}
		viaje.setOrigen(ubicacionService.obtenerOCrear(request.origen(), "origen"));
		viaje.setDestino(ubicacionService.obtenerOCrear(request.destino(), "destino"));
		viaje.setEstado(estado(PENDIENTE));
		viaje.setFhAgenda(agenda);
		viaje.setNumeroVuelo(request.numeroVuelo());
		viajeRepository.saveAndFlush(viaje);
		entityManager.refresh(viaje);
		return aResponse(viaje);
	}

	@Transactional
	public ViajeDTOResponse asignar(String folio, AsignarViajeDTORequest request) {
		Viaje viaje = viajeRepository.bloquearPorFolio(folio)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		Conductor conductor = conductorService.bloquear(request.correoConductor());
		Movil movil = movilService.bloquear(request.patente());
		entityManager.refresh(conductor, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
		entityManager.refresh(movil, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
		exigirEstado(viaje, PENDIENTE, ASIGNADO);
		LocalDateTime ahora = LocalDateTime.now(clock);
		if (ahora.isAfter(viaje.getFhAgenda().plusHours(1))) {
			throw conflicto("El viaje está vencido");
		}
		int pasajeros = Objects.requireNonNullElse(viaje.getCantidadPasajeros(), 0);
		if (pasajeros == 0) {
			throw conflicto("El viaje no tiene pasajeros");
		}
		Conductor anterior = viaje.getConductor();
		boolean cambiaConductor = anterior == null || !anterior.getIdUsuario().equals(conductor.getIdUsuario());
		boolean cambiaMovil = viaje.getMovil() == null || !viaje.getMovil().getIdMovil().equals(movil.getIdMovil());
		if (!cambiaConductor && !cambiaMovil) {
			return aResponse(viaje);
		}
		if (cambiaConductor) {
			if (!conductor.isActivo()) {
				throw conflicto("El conductor no está activo");
			}
			if (!ConductorService.DISPONIBLE.equals(conductor.getEstadoConductor().getEstado())) {
				throw conflicto("El conductor no está disponible");
			}
		}
		if (cambiaMovil) {
			if (!movil.isActivo()) {
				throw conflicto("El móvil no está activo");
			}
			if (!MovilService.DISPONIBLE.equals(movil.getEstadoMovil().getNombre())) {
				throw conflicto("El móvil no está disponible");
			}
		}
		if (!detalleMovilConductorRepository.existsById(
				new DetalleMovilConductorId(movil.getIdMovil(), conductor.getIdUsuario()))) {
			throw conflicto("El conductor no está habilitado para el móvil");
		}
		if (cambiaMovil && pasajeros > movil.getCapacidad()) {
			throw conflicto("Los pasajeros superan la capacidad del móvil");
		}
		if (cambiaConductor && haySolape(viaje, viajeRepository.findByConductorIdUsuario(conductor.getIdUsuario()))) {
			throw conflicto("El conductor ya tiene un viaje a menos de 2 horas");
		}
		if (cambiaMovil && haySolape(viaje, viajeRepository.findByMovilIdMovil(movil.getIdMovil()))) {
			throw conflicto("El móvil ya tiene un viaje a menos de 2 horas");
		}
		viaje.setConductor(conductor);
		viaje.setMovil(movil);
		viaje.setEstado(estado(ASIGNADO));
		viaje.setFhAsignacion(ahora);
		viaje.setMotivo(null);
		if (cambiaMovil) {
			limpiarAbordaje(viaje);
		}
		avisar(conductor.getUsuario().getTokenFcm(), AvisosDeViaje.nuevoViaje(viaje.getFolio(), viaje.getFhAgenda()));
		avisar(viaje.getSolicitante().getUsuario().getTokenFcm(), AvisosDeViaje.viajeAsignado(viaje.getFolio()));
		if (cambiaConductor && anterior != null) {
			avisar(anterior.getUsuario().getTokenFcm(), AvisosDeViaje.viajeReasignado(viaje.getFolio()));
		}
		return aResponse(viaje);
	}

	private void limpiarAbordaje(Viaje viaje) {
		detalleViajePasajeroRepository.findByIdViaje(viaje.getIdViaje())
				.forEach(detalle -> detalle.setFhAbordaje(null));
	}

	private void avisar(String tokenFcm, AvisosDeViaje.Aviso aviso) {
		firebaseClient.notificar(tokenFcm, aviso.titulo(), aviso.mensaje(), aviso.datos());
	}

	private static boolean haySolape(Viaje viaje, List<Viaje> deLaMismaFlota) {
		LocalDateTime desde = viaje.getFhAgenda().minusHours(2);
		LocalDateTime hasta = viaje.getFhAgenda().plusHours(2);
		return deLaMismaFlota.stream()
				.filter(otro -> !otro.getIdViaje().equals(viaje.getIdViaje()))
				.filter(otro -> Set.of(ASIGNADO, EN_CURSO).contains(otro.getEstado().getNombreEstado()))
				.anyMatch(otro -> otro.getFhAgenda().isAfter(desde) && otro.getFhAgenda().isBefore(hasta));
	}

	private static ResponseStatusException conflicto(String mensaje) {
		return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
	}

	static boolean dentroDeLaVentanaDeInicio(LocalDateTime fhAgenda, LocalDateTime ahora) {
		return !ahora.isBefore(fhAgenda.minusHours(1)) && !ahora.isAfter(fhAgenda.plusHours(1));
	}

	private void exigirPuedeIniciar(Viaje viaje, LocalDateTime ahora) {
		if (!dentroDeLaVentanaDeInicio(viaje.getFhAgenda(), ahora)) {
			throw conflicto("El viaje está fuera de la ventana de inicio");
		}
		if (hayOtroEnCurso(viaje, viajeRepository.findByConductorIdUsuario(viaje.getConductor().getIdUsuario()))) {
			throw conflicto("El conductor ya tiene un viaje en curso");
		}
		if (hayOtroEnCurso(viaje, viajeRepository.findByMovilIdMovil(viaje.getMovil().getIdMovil()))) {
			throw conflicto("El móvil ya tiene un viaje en curso");
		}
	}

	private static boolean hayOtroEnCurso(Viaje viaje, List<Viaje> deLaMismaFlota) {
		return deLaMismaFlota.stream()
				.filter(otro -> !otro.getIdViaje().equals(viaje.getIdViaje()))
				.anyMatch(otro -> EN_CURSO.equals(otro.getEstado().getNombreEstado()));
	}

	@Transactional
	public ViajeDTOResponse cambiarEstado(String folio, CambiarEstadoViajeDTORequest request, String correoConductor) {
		Viaje viaje = viajeRepository.bloquearPorFolio(folio)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (viaje.getConductor() == null
				|| !viaje.getConductor().getUsuario().getCorreo().equalsIgnoreCase(correoConductor)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		String actual = viaje.getEstado().getNombreEstado();
		String nuevo = request.estado().toUpperCase(Locale.ROOT);
		if (EN_CURSO.equals(nuevo) && ASIGNADO.equals(actual)) {
			LocalDateTime ahora = LocalDateTime.now(clock);
			exigirPuedeIniciar(viaje, ahora);
			viaje.setFhInicio(ahora);
			conductorService.cambiarEstado(viaje.getConductor(), ConductorService.OCUPADO);
			movilService.cambiarEstado(viaje.getMovil(), MovilService.EN_USO);
		} else if (FINALIZADO.equals(nuevo) && EN_CURSO.equals(actual)) {
			viaje.setFhFin(LocalDateTime.now(clock));
			liberar(viaje);
		} else {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"No se puede pasar de " + actual + " a " + nuevo);
		}
		viaje.setEstado(estado(nuevo));
		firebaseClient.notificar(viaje.getSolicitante().getUsuario().getTokenFcm(),
				"Viaje " + viaje.getFolio(), "Estado: " + nuevo, Map.of("folio", viaje.getFolio()));
		return aResponse(viaje);
	}

	@Transactional
	public ViajeDTOResponse rechazar(String folio, RechazarViajeDTORequest request, String correoConductor) {
		Viaje viaje = viajeRepository.bloquearPorFolio(folio)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (viaje.getConductor() == null
				|| !viaje.getConductor().getUsuario().getCorreo().equalsIgnoreCase(correoConductor)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		exigirEstado(viaje, ASIGNADO);
		viaje.setConductor(null);
		viaje.setMovil(null);
		viaje.setFhAsignacion(null);
		viaje.setMotivo(request.motivo());
		viaje.setEstado(estado(PENDIENTE));
		limpiarAbordaje(viaje);
		administradorRepository.findAll().forEach(administrador -> avisar(
				administrador.getUsuario().getTokenFcm(), AvisosDeViaje.viajeRechazado(viaje.getFolio())));
		avisar(viaje.getSolicitante().getUsuario().getTokenFcm(), AvisosDeViaje.viajeSinConductor(viaje.getFolio()));
		return aResponse(viaje);
	}

	@Transactional
	public ViajeDTOResponse cancelar(String folio, String correoSolicitante) {
		Viaje viaje = viajeRepository.bloquearPorFolio(folio)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!viaje.getSolicitante().getUsuario().getCorreo().equalsIgnoreCase(correoSolicitante)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		exigirEstado(viaje, PENDIENTE, ASIGNADO);
		if (LocalDateTime.now(clock).isAfter(viaje.getFhAgenda().plusHours(1))) {
			throw conflicto("El viaje está vencido. Contacta al Administrador.");
		}
		viaje.setEstado(estado(CANCELADO));
		String estadoPagoAnterior = estadoDelPago(viaje);
		if (viaje.getPago() != null) {
			pagoService.cancelarPorSolicitante(viaje.getPago());
		}
		avisarCancelacion(viaje, estadoPagoAnterior, false);
		return aResponse(viaje);
	}

	@Transactional
	public ViajeDTOResponse cancelarPorAdministrador(String folio, CancelarViajeDTORequest request) {
		Viaje viaje = viajeRepository.bloquearPorFolio(folio)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		exigirEstado(viaje, PENDIENTE, ASIGNADO);
		boolean vencido = LocalDateTime.now(clock).isAfter(viaje.getFhAgenda().plusHours(1));
		Pago pago = viaje.getPago();
		int reembolso = pago == null ? 0
				: pagoService.reembolsoDelAdministrador(pago, vencido, request.montoReembolso());
		String estadoPagoAnterior = estadoDelPago(viaje);
		viaje.setMotivo(request.motivo());
		viaje.setEstado(estado(CANCELADO));
		if (pago != null) {
			pagoService.cancelarPorAdministrador(pago, reembolso);
		}
		avisarCancelacion(viaje, estadoPagoAnterior, true);
		return aResponse(viaje);
	}

	private static String estadoDelPago(Viaje viaje) {
		return viaje.getPago() == null ? null : viaje.getPago().getEstadoPago().getEstado();
	}

	private void avisarCancelacion(Viaje viaje, String estadoPagoAnterior, boolean avisarAlSolicitante) {
		String folio = viaje.getFolio();
		Map<String, String> datos = Map.of("tipo", "VIAJE", "folio", folio);
		String tokenSolicitante = viaje.getSolicitante().getUsuario().getTokenFcm();
		if (avisarAlSolicitante) {
			firebaseClient.notificar(tokenSolicitante,
					"Viaje cancelado", "Tu viaje " + folio + " fue cancelado", datos);
		}
		String estadoPago = estadoDelPago(viaje);
		if (estadoPago != null && !estadoPago.equals(estadoPagoAnterior)) {
			firebaseClient.notificar(tokenSolicitante, "Pago del viaje " + folio,
					"Estado: " + ("REEMBOLSADO".equals(estadoPago) ? "Reembolsado" : "Anulado"), datos);
		}
		if (viaje.getConductor() != null) {
			firebaseClient.notificar(viaje.getConductor().getUsuario().getTokenFcm(),
					"Viaje cancelado", "El viaje " + folio + " fue cancelado", datos);
		}
	}

	@Transactional(readOnly = true)
	public List<ViajeDTOResponse> listarDelAdministrador(FiltroViajes filtro) {
		List<Viaje> viajes = filtro.estado() == null
				? viajeRepository.findAll()
				: viajeRepository.findByEstadoNombreEstado(filtro.estado());
		return filtrados(viajes, filtro);
	}

	@Transactional(readOnly = true)
	public List<ViajeDTOResponse> listarDeSolicitante(String correoSolicitante, FiltroViajes filtro) {
		Long idUsuario = solicitanteService.buscarOFallar(correoSolicitante).getIdUsuario();
		return filtrados(viajeRepository.findBySolicitanteIdUsuario(idUsuario), filtro);
	}

	@Transactional(readOnly = true)
	public List<ViajeDTOResponse> listarDelConductor(String correoConductor, FiltroViajes filtro) {
		Long idUsuario = conductorService.buscarOFallar(correoConductor).getIdUsuario();
		List<Viaje> suyos = viajeRepository.findByConductorIdUsuario(idUsuario);
		if (filtro.estado() != null) {
			return filtrados(suyos, filtro);
		}
		LocalDate hoy = LocalDate.now(clock);
		LocalDate dia = filtro.fecha() == null ? hoy : filtro.fecha();
		return suyos.stream()
				.filter(viaje -> enLaAgenda(viaje, dia, hoy))
				.map(this::aResponse)
				.sorted(OrdenViajes.PROXIMOS_PRIMERO)
				.toList();
	}

	private static boolean enLaAgenda(Viaje viaje, LocalDate dia, LocalDate hoy) {
		String estado = viaje.getEstado().getNombreEstado();
		LocalDate agendado = viaje.getFhAgenda().toLocalDate();
		boolean delDia = AGENDA.contains(estado) && agendado.equals(dia);
		boolean enCursoAnterior = EN_CURSO.equals(estado) && dia.equals(hoy) && agendado.isBefore(hoy);
		return delDia || enCursoAnterior;
	}

	private List<ViajeDTOResponse> filtrados(List<Viaje> viajes, FiltroViajes filtro) {
		return viajes.stream()
				.filter(viaje -> filtro.estado() == null
						|| filtro.estado().equals(viaje.getEstado().getNombreEstado()))
				.filter(viaje -> filtro.fecha() == null || filtro.fecha().equals(viaje.getFhAgenda().toLocalDate()))
				.map(this::aResponse)
				.sorted(filtro.estado() == null ? OrdenViajes.RECIENTES_PRIMERO : OrdenViajes.paraEstado(filtro.estado()))
				.toList();
	}

	private void liberar(Viaje viaje) {
		if (viaje.getConductor() != null) {
			conductorService.cambiarEstado(viaje.getConductor(), ConductorService.DISPONIBLE);
		}
		if (viaje.getMovil() != null) {
			movilService.cambiarEstado(viaje.getMovil(), MovilService.DISPONIBLE);
		}
	}

	private void exigirEstado(Viaje viaje, String... permitidos) {
		String actual = viaje.getEstado().getNombreEstado();
		for (String permitido : permitidos) {
			if (permitido.equals(actual)) {
				return;
			}
		}
		throw new ResponseStatusException(HttpStatus.CONFLICT, "El viaje está " + actual);
	}

	private EstadoSolicitud estado(String nombre) {
		return estadoSolicitudRepository.findByNombreEstado(nombre)
				.orElseThrow(() -> new IllegalStateException("Falta el estado de solicitud " + nombre));
	}
}
