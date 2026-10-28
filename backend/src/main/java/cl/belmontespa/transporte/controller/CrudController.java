package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class CrudController<Q, R> extends LecturaController<R> {

	@Override
	protected abstract CrudService<?, String, Q, R> service();

	@PostMapping
	public org.springframework.http.ResponseEntity<R> crear(@Valid @RequestBody Q request, Authentication auth) {
		R respuesta = crearPara(request, auth);
		return org.springframework.http.ResponseEntity.status(estadoCreacion(respuesta)).body(respuesta);
	}

	protected HttpStatus estadoCreacion(R respuesta) { return HttpStatus.CREATED; }

	protected R crearPara(Q request, Authentication auth) {
		return service().crear(request);
	}
}
