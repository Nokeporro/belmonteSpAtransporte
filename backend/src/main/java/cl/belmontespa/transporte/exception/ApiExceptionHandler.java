package cl.belmontespa.transporte.exception;

import cl.belmontespa.transporte.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Set<String> OBLIGATORIEDAD = Set.of("NotNull", "NotBlank", "NotEmpty");

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> statusExplicito(ResponseStatusException e, HttpServletRequest request) {
		return error(e.getStatusCode(), e.getReason(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e, HttpServletRequest request) {
		Map<String, String> motivos = new TreeMap<>();
		e.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing((FieldError campo) -> !OBLIGATORIEDAD.contains(campo.getCode()))
						.thenComparing(campo -> Objects.toString(campo.getDefaultMessage(), "")))
				.forEach(campo -> motivos.putIfAbsent(campo.getField(),
						Objects.requireNonNullElse(campo.getDefaultMessage(), "Valor inválido.")));
		String lineas = motivos.entrySet().stream()
				.map(motivo -> motivo.getKey() + ": " + motivo.getValue())
				.collect(Collectors.joining("\n"));
		return error(HttpStatus.BAD_REQUEST, lineas, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> cuerpoInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Cuerpo inválido", request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException e, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, null, request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> integridad(DataIntegrityViolationException e, HttpServletRequest request) {
		log.warn("Violación de integridad en {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
		return error(HttpStatus.CONFLICT, "La operación viola una restricción de datos", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> inesperado(Exception e, HttpServletRequest request) {
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
