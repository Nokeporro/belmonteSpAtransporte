package com.belmontespa.transporte.exception;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String,Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "timestamp", Instant.now(), "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Correo o contraseña inválidos"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> handle(ResponseStatusException ex) { return ResponseEntity.status(ex.getStatusCode()).body(Map.of("timestamp", Instant.now(), "status", ex.getStatusCode().value(), "error", ex.getReason() == null ? "Error de solicitud" : ex.getReason())); }
}
