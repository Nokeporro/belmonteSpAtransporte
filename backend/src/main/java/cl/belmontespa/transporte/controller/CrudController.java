package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lo de LecturaController más el POST común a los recursos que se crean por la API.
 *
 * @param <Q> DTORequest
 * @param <R> DTOResponse
 */
public abstract class CrudController<Q, R> extends LecturaController<R> {

	@Override
	protected abstract CrudService<?, String, Q, R> service();

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public R crear(@Valid @RequestBody Q request, Authentication auth) {
		return crearPara(request, auth);
	}

	protected R crearPara(Q request, Authentication auth) {
		return service().crear(request);
	}
}
