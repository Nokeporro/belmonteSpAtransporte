package cl.belmontespa.transporte.exception;

import cl.belmontespa.transporte.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// Toda respuesta de error de la API sale como ApiError. Los 401/403 del filtro de
// seguridad (sin token, rol insuficiente por ruta) no llegan aquí.
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> statusExplicito(ResponseStatusException e, HttpServletRequest request) {
		return error(e.getStatusCode(), e.getReason(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e, HttpServletRequest request) {
		String campos = e.getBindingResult().getFieldErrors().stream()
				.map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return error(HttpStatus.BAD_REQUEST, campos, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> cuerpoInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Cuerpo inválido", request);
	}

	// @PreAuthorize en un controller: el recurso no es del usuario autenticado.
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException e, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, null, request);
	}

	// Carrera con un UNIQUE o rechazo de un trigger (rol activo, capacidad del móvil).
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> integridad(DataIntegrityViolationException e, HttpServletRequest request) {
		log.warn("Violación de integridad en {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
		return error(HttpStatus.CONFLICT, "La operación viola una restricción de datos", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> inesperado(Exception e, HttpServletRequest request) {
		// Excepciones de Spring MVC (404 de ruta, 405, parámetro inválido) ya traen su status.
		if (e instanceof ErrorResponse respuesta) {
			return error(respuesta.getStatusCode(), respuesta.getBody().getDetail(), request);
		}
		log.error("Error no controlado en {}", request.getRequestURI(), e);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", request);
	}

	private static ResponseEntity<ApiError> error(HttpStatusCode status, String mensaje, HttpServletRequest request) {
		HttpStatus http = HttpStatus.resolve(status.value());
		String nombre = http == null ? String.valueOf(status.value()) : http.getReasonPhrase();
		return ResponseEntity.status(status).body(new ApiError(
				Instant.now(), status.value(), nombre, mensaje, request.getRequestURI()));
	}
}
