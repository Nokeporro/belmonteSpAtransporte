package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.ConductorDTORequest;
import cl.belmontespa.transporte.dto.response.ConductorDTOResponse;
import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.EstadoConductor;
import cl.belmontespa.transporte.repository.ConductorRepository;
import cl.belmontespa.transporte.repository.EstadoConductorRepository;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ConductorService extends CrudService<Conductor, String, ConductorDTORequest, ConductorDTOResponse> {

	static final String DISPONIBLE = "DISPONIBLE";
	static final String OCUPADO = "OCUPADO";

	private final ConductorRepository conductorRepository;
	private final EstadoConductorRepository estadoConductorRepository;
	private final UsuarioService usuarioService;

	public ConductorService(ConductorRepository conductorRepository,
			EstadoConductorRepository estadoConductorRepository, UsuarioService usuarioService) {
		this.conductorRepository = conductorRepository;
		this.estadoConductorRepository = estadoConductorRepository;
		this.usuarioService = usuarioService;
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
		usuarioService.reemplazarDatos(conductor.getUsuario(),
				request.nombre(), request.correo(), request.telefono(), request.password());
	}

	@Override
	protected ConductorDTOResponse aResponse(Conductor conductor) {
		return ConductorDTOResponse.from(conductor);
	}

	void cambiarEstado(Conductor conductor, String nombreEstado) {
		conductor.setEstadoConductor(estado(nombreEstado));
	}

	private EstadoConductor estado(String nombre) {
		return estadoConductorRepository.findByEstado(nombre)
				.orElseThrow(() -> new IllegalStateException("Falta el estado de conductor " + nombre));
	}
}
