package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.UsuarioDTORequest;
import cl.belmontespa.transporte.dto.response.UsuarioDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController extends LecturaController<UsuarioDTOResponse> {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@Override
	protected CrudService<?, String, ?, UsuarioDTOResponse> service() {
		return usuarioService;
	}

	@Override
	protected List<UsuarioDTOResponse> listarPara(Authentication auth, Map<String, String> filtros) {
		return List.of(usuarioService.obtener(auth.getName()));
	}

	@Override
	protected void exigirAccesoPrevio(String correo, Authentication auth) {
		exigirPropio(correo, auth);
	}

	@PutMapping("/{clave}")
	@PreAuthorize("#clave.equalsIgnoreCase(authentication.name)")
	public UsuarioDTOResponse actualizar(@PathVariable String clave, @Valid @RequestBody UsuarioDTORequest request) {
		return usuarioService.actualizar(clave, request);
	}
}
