package cl.belmontespa.transporte.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD común a los recursos de la API. Opera con DTO y la clave de negocio de la
 * ruta, nunca con el id interno ni con el Model.
 *
 * @param <E> entidad
 * @param <K> clave de ruta (correo, rut, patente, ...)
 * @param <Q> DTORequest
 * @param <R> DTOResponse
 */
public abstract class CrudService<E, K, Q, R> {

	protected abstract JpaRepository<E, ?> repositorio();

	protected abstract Optional<E> buscar(K clave);

	protected abstract R aResponse(E entidad);

	// POST solo existe donde el README lo declara; en usuarios y empresas la cuenta o la
	// fila nace por su rol, así que no hay ruta que llegue aquí.
	protected E nuevaEntidad(Q request) {
		throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
	}

	// PUT solo existe en usuarios y conductores (README, "CRUD REST").
	protected void reemplazar(E entidad, Q request) {
		throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
	}

	// Orden fijo de la lista (README, "Consultas → Orden"). Los textos los ordena la base, con la
	// collation de la columna (utf8mb4_unicode_ci); el último criterio es la clave única, solo para
	// que el orden sea determinista.
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
