package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.client.EmailClient;
import cl.belmontespa.transporte.dto.request.SolicitanteDTORequest;
import cl.belmontespa.transporte.dto.response.SolicitanteDTOResponse;
import cl.belmontespa.transporte.model.EstadoSolicitante;
import cl.belmontespa.transporte.model.Solicitante;
import cl.belmontespa.transporte.repository.EstadoSolicitanteRepository;
import cl.belmontespa.transporte.repository.SolicitanteRepository;
import cl.belmontespa.transporte.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitanteService
		extends CrudService<Solicitante, String, SolicitanteDTORequest, SolicitanteDTOResponse> {

	static final String APROBADO = "APROBADO";

	private final SolicitanteRepository solicitanteRepository;
	private final EstadoSolicitanteRepository estadoSolicitanteRepository;
	private final UsuarioService usuarioService;
	private final EmpresaService empresaService;
	private final UsuarioRepository usuarioRepository;
	private final EmailClient emailClient;

	public SolicitanteService(SolicitanteRepository solicitanteRepository,
			EstadoSolicitanteRepository estadoSolicitanteRepository, UsuarioService usuarioService,
			EmpresaService empresaService, UsuarioRepository usuarioRepository, EmailClient emailClient) {
		this.solicitanteRepository = solicitanteRepository;
		this.estadoSolicitanteRepository = estadoSolicitanteRepository;
		this.usuarioService = usuarioService;
		this.empresaService = empresaService;
		this.usuarioRepository = usuarioRepository;
		this.emailClient = emailClient;
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

	@Transactional(readOnly = true)
	public List<SolicitanteDTOResponse> listarConEstado(String filtro) {
		if (filtro == null || filtro.isBlank()) return listar();
		String nombre = filtro.strip();
		if (!List.of("PENDIENTE", APROBADO, "RECHAZADO").contains(nombre)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estado: Debe ser PENDIENTE, APROBADO o RECHAZADO.");
		}
		return solicitanteRepository.findByEstadoSolicitanteEstado(nombre, orden()).stream().map(this::aResponse).toList();
	}

	@Transactional
	public SolicitanteDTOResponse revisar(String correo, boolean aprobado) {
		var usuario = usuarioRepository.bloquearPorCorreo(correo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		Solicitante solicitante = solicitanteRepository.findById(usuario.getIdUsuario())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!"PENDIENTE".equals(solicitante.getEstadoSolicitante().getEstado())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya fue revisada.");
		}
		solicitante.setEstadoSolicitante(estado(aprobado ? APROBADO : "RECHAZADO"));
		emailClient.enviarCuentaRevisada(usuario.getCorreo(), usuario.getNombre(), solicitante.getEmpresa().getRazonSocial(), aprobado);
		return aResponse(solicitante);
	}

	private EstadoSolicitante estado(String nombre) {
		return estadoSolicitanteRepository.findByEstado(nombre)
				.orElseThrow(() -> new IllegalStateException("Falta el estado de solicitante " + nombre));
	}
}
