package cl.belmontespa.transporte.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

public abstract class CrudService<E, K, Q, R> {

	protected abstract JpaRepository<E, ?> repositorio();

	protected abstract Optional<E> buscar(K clave);

	protected abstract R aResponse(E entidad);

	protected E nuevaEntidad(Q request) {
		throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
	}

	protected void reemplazar(E entidad, Q request) {
		throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
	}

	protected Sort orden() {
		return Sort.unsorted();
	}

	@Transactional(readOnly = true)
	public List<R> listar() {
		return repositorio().findAll(orden()).stream().map(this::aResponse).toList();
	}

	@Transactional(readOnly = true)
	public R obtener(K clave) {
		return aResponse(buscarOFallar(clave));
	}

	@Transactional
	public R crear(Q request) {
		return aResponse(repositorio().save(nuevaEntidad(request)));
	}

	@Transactional
	public R actualizar(K clave, Q request) {
		E entidad = buscarOFallar(clave);
		reemplazar(entidad, request);
		return aResponse(entidad);
	}

	protected E buscarOFallar(K clave) {
		return buscar(clave).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
