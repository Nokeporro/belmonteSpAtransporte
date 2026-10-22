package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.client.FirebaseClient;
import cl.belmontespa.transporte.dto.request.AsignarViajeDTORequest;
import cl.belmontespa.transporte.dto.request.CambiarEstadoViajeDTORequest;
import cl.belmontespa.transporte.dto.request.ViajeDTORequest;
import cl.belmontespa.transporte.dto.response.ViajeDTOResponse;
import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.DetalleMovilConductorId;
import cl.belmontespa.transporte.model.EstadoSolicitud;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.model.Viaje;
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

// Conductor, móvil y estado no se cambian por PUT, solo por las acciones de negocio
// (README, "Acciones de negocio"); los triggers de VIAJE registran cada cambio en el historial.
@Service
public class ViajeService extends CrudService<Viaje, String, ViajeDTORequest, ViajeDTOResponse> {

	static final String PENDIENTE = "PENDIENTE";
	static final String ASIGNADO = "ASIGNADO";
	static final String EN_CURSO = "EN_CURSO";
	static final String FINALIZADO = "FINALIZADO";
	static final String CANCELADO = "CANCELADO";

	// Estados que entran a la agenda del Conductor.
	private static final Set<String> AGENDA = Set.of(ASIGNADO, EN_CURSO, FINALIZADO);

	private final ViajeRepository viajeRepository;
	private final EstadoSolicitudRepository estadoSolicitudRepository;
	private final DetalleViajePasajeroRepository detalleViajePasajeroRepository;
	private final DetalleMovilConductorRepository detalleMovilConductorRepository;
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
			DetalleMovilConductorRepository detalleMovilConductorRepository,
			SolicitanteService solicitanteService, ConductorService conductorService, MovilService movilService,
			PagoService pagoService, UbicacionService ubicacionService, FirebaseClient firebaseClient,
			EntityManager entityManager, Clock clock) {
		this.viajeRepository = viajeRepository;
		this.estadoSolicitudRepository = estadoSolicitudRepository;
		this.detalleViajePasajeroRepository = detalleViajePasajeroRepository;
		this.detalleMovilConductorRepository = detalleMovilConductorRepository;
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

	// El viaje necesita al Solicitante autenticado: usar crear(request, correoSolicitante).
	@Override
	protected Viaje nuevaEntidad(ViajeDTORequest request) {
		throw new IllegalStateException("Usar ViajeService.crear(request, correoSolicitante)");
	}

	@Override
	protected ViajeDTOResponse aResponse(Viaje viaje) {
		return ViajeDTOResponse.from(viaje, detalleViajePasajeroRepository.findByIdViaje(viaje.getIdViaje()));
	}

	// CU-01/RF02: la solicitud nace Pendiente; el folio lo genera Viaje.@PrePersist.
	@Transactional
	public ViajeDTOResponse crear(ViajeDTORequest request, String correoSolicitante) {
		Viaje viaje = new Viaje();
		viaje.setSolicitante(solicitanteService.buscarOFallar(correoSolicitante));
		viaje.setOrigen(ubicacionService.obtenerOCrear(request.origen()));
		viaje.setDestino(ubicacionService.obtenerOCrear(request.destino()));
		viaje.setEstado(estado(PENDIENTE));
		viaje.setFhAgenda(request.fhAgenda());
		viaje.setNumeroVuelo(request.numeroVuelo());
		viajeRepository.saveAndFlush(viaje);
		// fh_creacion y cantidad_pasajeros los completa la base de datos.
		entityManager.refresh(viaje);
		return aResponse(viaje);
	}

	// CU-04/RF05: también reasigna un viaje Asignado (HU05), liberando al conductor y móvil previos.
	@Transactional
	public ViajeDTOResponse asignar(String folio, AsignarViajeDTORequest request) {
		Viaje viaje = buscarOFallar(folio);
		exigirEstado(viaje, PENDIENTE, ASIGNADO);
		Conductor conductor = conductorService.buscarOFallar(request.correoConductor());
		Movil movil = movilService.buscarOFallar(request.patente());
		boolean mismoConductor = viaje.getConductor() != null
				&& viaje.getConductor().getIdUsuario().equals(conductor.getIdUsuario());
		boolean mismoMovil = viaje.getMovil() != null && viaje.getMovil().getIdMovil().equals(movil.getIdMovil());
		if (!mismoConductor && !ConductorService.DISPONIBLE.equals(conductor.getEstadoConductor().getEstado())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El conductor no está disponible");
		}
		if (!mismoMovil && !MovilService.DISPONIBLE.equals(movil.getEstadoMovil().getNombre())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El móvil no está disponible");
		}
		if (!detalleMovilConductorRepository.existsById(
				new DetalleMovilConductorId(movil.getIdMovil(), conductor.getIdUsuario()))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El conductor no está habilitado para el móvil");
		}
		if (Objects.requireNonNullElse(viaje.getCantidadPasajeros(), 0) > movil.getCapacidad()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Los pasajeros superan la capacidad del móvil");
		}
		liberar(viaje);
		viaje.setConductor(conductor);
		viaje.setMovil(movil);
		viaje.setEstado(estado(ASIGNADO));
		viaje.setFhAsignacion(LocalDateTime.now(clock));
		conductorService.cambiarEstado(conductor, ConductorService.OCUPADO);
		movilService.cambiarEstado(movil, MovilService.EN_USO);
		Map<String, String> datos = Map.of("folio", viaje.getFolio());
		firebaseClient.notificar(conductor.getUsuario().getTokenFcm(),
				"Nuevo viaje asignado", "Viaje " + viaje.getFolio() + " para " + viaje.getFhAgenda(), datos);
		firebaseClient.notificar(viaje.getSolicitante().getUsuario().getTokenFcm(),
				"Viaje asignado", "Tu viaje " + viaje.getFolio() + " ya tiene conductor y vehículo", datos);
		return aResponse(viaje);
	}

	// CU-05/RF09: Asignado → En Curso → Finalizado, solo por el conductor del viaje.
	@Transactional
	public ViajeDTOResponse cambiarEstado(String folio, CambiarEstadoViajeDTORequest request, String correoConductor) {
		Viaje viaje = buscarOFallar(folio);
		if (viaje.getConductor() == null
				|| !viaje.getConductor().getUsuario().getCorreo().equalsIgnoreCase(correoConductor)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		String actual = viaje.getEstado().getNombreEstado();
		String nuevo = request.estado().toUpperCase(Locale.ROOT);
		if (EN_CURSO.equals(nuevo) && ASIGNADO.equals(actual)) {
			viaje.setFhInicio(LocalDateTime.now(clock));
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

	// CU-08/RF15: libera conductor y móvil, anula el pago y avisa al conductor.
	@Transactional
	public ViajeDTOResponse cancelar(String folio, String correoSolicitante) {
		Viaje viaje = buscarOFallar(folio);
		if (!viaje.getSolicitante().getUsuario().getCorreo().equalsIgnoreCase(correoSolicitante)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		exigirEstado(viaje, PENDIENTE, ASIGNADO);
		viaje.setEstado(estado(CANCELADO));
		viaje.setFhFin(LocalDateTime.now(clock));
		liberar(viaje);
		if (viaje.getPago() != null) {
			pagoService.anular(viaje.getPago());
		}
		if (viaje.getConductor() != null) {
			firebaseClient.notificar(viaje.getConductor().getUsuario().getTokenFcm(),
					"Viaje cancelado", "El viaje " + viaje.getFolio() + " fue cancelado",
					Map.of("folio", viaje.getFolio()));
		}
		return aResponse(viaje);
	}

	// RF04: todos los viajes (con el estado y la fecha del filtro, si vienen).
	@Transactional(readOnly = true)
	public List<ViajeDTOResponse> listarDelAdministrador(FiltroViajes filtro) {
		List<Viaje> viajes = filtro.estado() == null
				? viajeRepository.findAll()
				: viajeRepository.findByEstadoNombreEstado(filtro.estado());
		return filtrados(viajes, filtro);
	}

	// RF03: historial del Solicitante, con los mismos filtros.
	@Transactional(readOnly = true)
	public List<ViajeDTOResponse> listarDeSolicitante(String correoSolicitante, FiltroViajes filtro) {
		Long idUsuario = solicitanteService.buscarOFallar(correoSolicitante).getIdUsuario();
		return filtrados(viajeRepository.findBySolicitanteIdUsuario(idUsuario), filtro);
	}

	// RF07: sin `estado`, la agenda de `fecha` (hoy por omisión): sus ASIGNADO, EN_CURSO y FINALIZADO
	// del día, más su EN_CURSO de un día anterior cuando la fecha es hoy, para que pueda finalizarlo.
	// Con `estado`, sus viajes en ese estado (y en esa fecha, si viene). Un viaje cancelado sale de la
	// agenda (CU-08).
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

	// El estado y la fecha se aplican aquí; el sentido del orden depende del estado (OrdenViajes).
	private List<ViajeDTOResponse> filtrados(List<Viaje> viajes, FiltroViajes filtro) {
		return viajes.stream()
				.filter(viaje -> filtro.estado() == null
						|| filtro.estado().equals(viaje.getEstado().getNombreEstado()))
				.filter(viaje -> filtro.fecha() == null || filtro.fecha().equals(viaje.getFhAgenda().toLocalDate()))
				.map(this::aResponse)
				.sorted(filtro.estado() == null ? OrdenViajes.RECIENTES_PRIMERO : OrdenViajes.paraEstado(filtro.estado()))
				.toList();
	}

	// Conductor y móvil vuelven a Disponible; el viaje conserva la referencia para el historial.
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
