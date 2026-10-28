package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.service.CrudService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

public abstract class LecturaController<R> {

	protected static final String ADM = "ADMINISTRADOR";
	protected static final String CON = "CONDUCTOR";
	protected static final String SOL = "SOLICITANTE";

	protected abstract CrudService<?, String, ?, R> service();

	@GetMapping
	public List<R> listar(Authentication auth, @RequestParam Map<String, String> filtros) {
		return listarPara(auth, filtros);
	}

	@GetMapping("/{clave}")
	public R obtener(@PathVariable String clave, Authentication auth) {
		validarClave(clave);
		exigirAccesoPrevio(clave, auth);
		R recurso = service().obtener(clave);
		exigirAcceso(clave, recurso, auth);
		return paraRol(recurso, auth);
	}

	protected List<R> listarPara(Authentication auth, Map<String, String> filtros) {
		return service().listar();
	}

	protected void validarClave(String clave) {
	}

	protected void exigirAccesoPrevio(String clave, Authentication auth) {
	}

	protected void exigirAcceso(String clave, R recurso, Authentication auth) {
	}

	protected R paraRol(R recurso, Authentication auth) {
		return recurso;
	}

	protected static void exigirFormato(boolean valido) {
		if (!valido) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	protected static boolean tieneRol(Authentication auth, String rol) {
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(authority -> ("ROLE_" + rol).equals(authority.getAuthority()));
	}

	protected static void exigirPropio(String correo, Authentication auth) {
		if (auth == null || correo == null || !correo.equalsIgnoreCase(auth.getName())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}
}
