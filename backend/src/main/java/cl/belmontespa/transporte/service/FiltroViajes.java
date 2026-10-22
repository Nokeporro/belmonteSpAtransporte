package cl.belmontespa.transporte.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Filtros de GET /viajes (README, "Consultas"): `estado` y `fecha` (AAAA-MM-DD), combinables. Un
// valor en blanco cuenta como ausente, uno mal formado es un 400 con una línea `campo: motivo` por
// filtro, y los parámetros que no son filtros se ignoran.
public record FiltroViajes(String estado, LocalDate fecha) {

	static final Set<String> ESTADOS = Set.of("PENDIENTE", "ASIGNADO", "EN_CURSO", "FINALIZADO", "CANCELADO");

	public static FiltroViajes de(Map<String, String> parametros) {
		String estado = valor(parametros, "estado");
		String texto = valor(parametros, "fecha");
		List<String> errores = new ArrayList<>();
		if (estado != null && !ESTADOS.contains(estado)) {
			errores.add("estado: Debe ser PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO o CANCELADO.");
		}
		LocalDate fecha = null;
		if (texto != null) {
			try {
				fecha = LocalDate.parse(texto);
			} catch (DateTimeParseException e) {
				errores.add("fecha: Debe tener el formato AAAA-MM-DD.");
			}
		}
		if (!errores.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("\n", errores));
		}
		return new FiltroViajes(estado, fecha);
	}

	private static String valor(Map<String, String> parametros, String nombre) {
		String valor = parametros.get(nombre);
		return valor == null || valor.isBlank() ? null : valor.strip();
	}
}
