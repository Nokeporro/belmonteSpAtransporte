package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.SolicitanteDTORequest;
import cl.belmontespa.transporte.dto.response.SolicitanteDTOResponse;
import cl.belmontespa.transporte.model.EstadoSolicitante;
import cl.belmontespa.transporte.model.Solicitante;
import cl.belmontespa.transporte.repository.EstadoSolicitanteRepository;
import cl.belmontespa.transporte.repository.SolicitanteRepository;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class SolicitanteService
		extends CrudService<Solicitante, String, SolicitanteDTORequest, SolicitanteDTOResponse> {

	static final String APROBADO = "APROBADO";

	private final SolicitanteRepository solicitanteRepository;
	private final EstadoSolicitanteRepository estadoSolicitanteRepository;
	private final UsuarioService usuarioService;
	private final EmpresaService empresaService;

	public SolicitanteService(SolicitanteRepository solicitanteRepository,
			EstadoSolicitanteRepository estadoSolicitanteRepository, UsuarioService usuarioService,
			EmpresaService empresaService) {
		this.solicitanteRepository = solicitanteRepository;
		this.estadoSolicitanteRepository = estadoSolicitanteRepository;
		this.usuarioService = usuarioService;
		this.empresaService = empresaService;
	}

	@Override
	protected JpaRepository<Solicitante, Long> repositorio() {
		return solicitanteRepository;
	}

	@Override
	protected Sort orden() {
		return Sort.by("usuario.nombre", "usuario.correo");
	}

	@Override
	protected Optional<Solicitante> buscar(String correo) {
		return solicitanteRepository.findByUsuarioCorreoIgnoreCase(correo);
	}

	// RF01: pertenecer a una empresa existente requiere revisión del Administrador.
	@Override
	protected Solicitante nuevaEntidad(SolicitanteDTORequest request) {
		Solicitante solicitante = new Solicitante();
		boolean existente = empresaService.existe(request.rutEmpresa());
		solicitante.setUsuario(usuarioService.nuevo(
				request.nombre(), request.correo(), request.telefono(), request.password()));
		solicitante.setEmpresa(empresaService.obtenerOCrear(request.razonSocial(), request.rutEmpresa()));
		solicitante.setEstadoSolicitante(estado(existente ? "PENDIENTE" : APROBADO));
		return solicitante;
	}

	@Override
	protected SolicitanteDTOResponse aResponse(Solicitante solicitante) {
		return SolicitanteDTOResponse.from(solicitante);
	}

	private EstadoSolicitante estado(String nombre) {
		return estadoSolicitanteRepository.findByEstado(nombre)
				.orElseThrow(() -> new IllegalStateException("Falta el estado de solicitante " + nombre));
	}
}
