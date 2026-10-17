package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.AdministradorDTORequest;
import cl.belmontespa.transporte.dto.response.AdministradorDTOResponse;
import cl.belmontespa.transporte.model.Administrador;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

// Sin PUT (README, "CRUD REST").
@Service
public class AdministradorService
		extends CrudService<Administrador, String, AdministradorDTORequest, AdministradorDTOResponse> {

	private final AdministradorRepository administradorRepository;
	private final UsuarioService usuarioService;

	public AdministradorService(AdministradorRepository administradorRepository, UsuarioService usuarioService) {
		this.administradorRepository = administradorRepository;
		this.usuarioService = usuarioService;
	}

	@Override
	protected JpaRepository<Administrador, Long> repositorio() {
		return administradorRepository;
	}

	@Override
	protected Optional<Administrador> buscar(String correo) {
		return administradorRepository.findByUsuarioCorreoIgnoreCase(correo);
	}

	@Override
	protected Administrador nuevaEntidad(AdministradorDTORequest request) {
		Administrador administrador = new Administrador();
		administrador.setUsuario(usuarioService.nuevo(
				request.nombre(), request.correo(), request.telefono(), request.password()));
		return administrador;
	}

	@Override
	protected AdministradorDTOResponse aResponse(Administrador administrador) {
		return AdministradorDTOResponse.from(administrador);
	}
}
