package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.ConductorDTORequest;
import cl.belmontespa.transporte.dto.response.ConductorDTOResponse;
import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.EstadoConductor;
import cl.belmontespa.transporte.repository.ConductorRepository;
import cl.belmontespa.transporte.repository.EstadoConductorRepository;
import cl.belmontespa.transporte.repository.ViajeRepository;
import cl.belmontespa.transporte.repository.ViajeRepository.ResumenConductor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConductorService extends CrudService<Conductor, String, ConductorDTORequest, ConductorDTOResponse> {

	static final String DISPONIBLE = "DISPONIBLE";
	static final String OCUPADO = "OCUPADO";

	private final ConductorRepository conductorRepository;
	private final EstadoConductorRepository estadoConductorRepository;
	private final UsuarioService usuarioService;
	private final ViajeRepository viajeRepository;

	public ConductorService(ConductorRepository conductorRepository,
			EstadoConductorRepository estadoConductorRepository, UsuarioService usuarioService,
			ViajeRepository viajeRepository) {
		this.conductorRepository = conductorRepository;
		this.estadoConductorRepository = estadoConductorRepository;
		this.usuarioService = usuarioService;
		this.viajeRepository = viajeRepository;
	}

	@Override
	protected JpaRepository<Conductor, Long> repositorio() {
		return conductorRepository;
	}

	@Override
	protected Sort orden() {
		return Sort.by("usuario.nombre", "usuario.correo");
	}

	@Override
	protected Optional<Conductor> buscar(String correo) {
		return conductorRepository.findByUsuarioCorreoIgnoreCase(correo);
	}

	// CU-07: queda disponible de inmediato para autenticarse y ser asignado.
	@Override
	protected Conductor nuevaEntidad(ConductorDTORequest request) {
		Conductor conductor = new Conductor();
		conductor.setUsuario(usuarioService.nuevo(
				request.nombre(), request.correo(), request.telefono(), request.password()));
		conductor.setEstadoConductor(estado(DISPONIBLE));
		return conductor;
	}

	// El Administrador reemplaza los datos y la contraseña del Conductor (CU-07/RF14).
	@Override
	protected void reemplazar(Conductor conductor, ConductorDTORequest request) {
		if (!conductor.getUsuario().getCorreo().equalsIgnoreCase(request.correo())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "correo: Debe coincidir con el de la ruta.");
		}
		usuarioService.reemplazarDatos(conductor.getUsuario(),
				request.nombre(), request.correo(), request.telefono(), request.password());
	}

	@Override
	protected ConductorDTOResponse aResponse(Conductor conductor) {
		ResumenConductor resumen = viajeRepository.resumirConductores(List.of(conductor.getIdUsuario()))
				.stream().findFirst().orElse(null);
		return respuesta(conductor, resumen);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ConductorDTOResponse> listar() {
		List<Conductor> conductores = conductorRepository.findAll(orden());
		if (conductores.isEmpty()) return List.of();
		Map<Long, ResumenConductor> resumenes = viajeRepository.resumirConductores(
				conductores.stream().map(Conductor::getIdUsuario).toList()).stream()
				.collect(Collectors.toMap(ResumenConductor::getIdConductor, Function.identity()));
		return conductores.stream().map(c -> respuesta(c, resumenes.get(c.getIdUsuario()))).toList();
	}

	private ConductorDTOResponse respuesta(Conductor conductor, ResumenConductor resumen) {
		return ConductorDTOResponse.from(conductor, resumen == null ? 0 : resumen.getViajesAsignados(),
				resumen == null ? null : resumen.getFolioEnCurso());
	}

	void cambiarEstado(Conductor conductor, String nombreEstado) {
		conductor.setEstadoConductor(estado(nombreEstado));
	}

	private EstadoConductor estado(String nombre) {
		return estadoConductorRepository.findByEstado(nombre)
				.orElseThrow(() -> new IllegalStateException("Falta el estado de conductor " + nombre));
	}
}
