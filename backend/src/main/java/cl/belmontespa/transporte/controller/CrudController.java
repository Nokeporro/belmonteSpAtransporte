package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.service.CrudService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * GET (listar), GET /{clave} y POST comunes a los recursos cuya clave de ruta es una
 * sola columna UNIQUE. Los roles por ruta los aplica SecurityConfig; el alcance "solo lo
 * propio" lo agrega cada controller con los hooks.
 *
 * @param <Q> DTORequest
 * @param <R> DTOResponse
 */
public abstract class CrudController<Q, R> {

	protected static final String ADM = "ADMINISTRADOR";
	protected static final String CON = "CONDUCTOR";
	protected static final String SOL = "SOLICITANTE";

	protected abstract CrudService<?, String, Q, R> service();

	@GetMapping
	public List<R> listar(Authentication auth, @RequestParam Map<String, String> filtros) {
		return listarPara(auth, filtros);
	}

	@GetMapping("/{clave}")
	public R obtener(@PathVariable String clave, Authentication auth) {
		R recurso = service().obtener(clave);
		exigirAcceso(clave, recurso, auth);
		return recurso;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public R crear(@Valid @RequestBody Q request, Authentication auth) {
		return crearPara(request, auth);
	}

	protected List<R> listarPara(Authentication auth, Map<String, String> filtros) {
		return service().listar();
	}

	protected void exigirAcceso(String clave, R recurso, Authentication auth) {
	}

	protected R crearPara(Q request, Authentication auth) {
		return service().crear(request);
	}

	protected static boolean tieneRol(Authentication auth, String rol) {
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(authority -> ("ROLE_" + rol).equals(authority.getAuthority()));
	}

	// 403 si el correo no es el del usuario autenticado (RNF08).
	protected static void exigirPropio(String correo, Authentication auth) {
		if (auth == null || correo == null || !correo.equalsIgnoreCase(auth.getName())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}
}
