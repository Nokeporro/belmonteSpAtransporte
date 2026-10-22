package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.SolicitanteDTORequest;
import cl.belmontespa.transporte.dto.response.SolicitanteDTOResponse;
import cl.belmontespa.transporte.model.EstadoSolicitante;
import cl.belmontespa.transporte.model.Solicitante;
import cl.belmontespa.transporte.repository.EstadoSolicitanteRepository;
import cl.belmontespa.transporte.repository.SolicitanteRepository;
import java.util.Optional;
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
	protected Optional<Solicitante> buscar(String correo) {
		return solicitanteRepository.findByUsuarioCorreoIgnoreCase(correo);
	}

	// RF01: la razón social y el RUT crean o reutilizan la empresa. Todo registro queda aprobado;
	// la revisión de las empresas existentes (RF26) se agrega después.
	@Override
	protected Solicitante nuevaEntidad(SolicitanteDTORequest request) {
		Solicitante solicitante = new Solicitante();
		solicitante.setUsuario(usuarioService.nuevo(
				request.nombre(), request.correo(), request.telefono(), request.password()));
		solicitante.setEmpresa(empresaService.obtenerOCrear(request.razonSocial(), request.rutEmpresa()));
		solicitante.setEstadoSolicitante(estado(APROBADO));
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
