package cl.belmontespa.transporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("Filtros de GET /viajes (README, Consultas)")
class FiltroViajesTest {

	private static void assert400(Map<String, String> parametros, String mensaje) {
		assertThatThrownBy(() -> FiltroViajes.de(parametros))
				.isInstanceOfSatisfying(ResponseStatusException.class, e -> {
					assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(e.getReason()).isEqualTo(mensaje);
				});
	}

	@Test
	@DisplayName("sin parámetros no hay filtros")
	void sinFiltros() {
		assertThat(FiltroViajes.de(Map.of())).isEqualTo(new FiltroViajes(null, null));
	}

	@Test
	@DisplayName("estado y fecha se combinan y cada estado válido se acepta")
	void validos() {
		assertThat(FiltroViajes.de(Map.of("estado", "EN_CURSO", "fecha", "2026-10-28")))
				.isEqualTo(new FiltroViajes("EN_CURSO", LocalDate.of(2026, 10, 28)));
		for (String estado : new String[] {"PENDIENTE", "ASIGNADO", "EN_CURSO", "FINALIZADO", "CANCELADO"}) {
			assertThat(FiltroViajes.de(Map.of("estado", estado)).estado()).isEqualTo(estado);
		}
	}

	@Test
	@DisplayName("un valor en blanco cuenta como ausente y se recortan los espacios")
	void blancos() {
		assertThat(FiltroViajes.de(Map.of("estado", "", "fecha", "  "))).isEqualTo(new FiltroViajes(null, null));
		assertThat(FiltroViajes.de(Map.of("estado", " PENDIENTE "))).isEqualTo(new FiltroViajes("PENDIENTE", null));
	}

	@Test
	@DisplayName("los parámetros que no son filtros se ignoran")
	void desconocidos() {
		assertThat(FiltroViajes.de(Map.of("foo", "bar", "estado", "ASIGNADO")))
				.isEqualTo(new FiltroViajes("ASIGNADO", null));
	}

	@Test
	@DisplayName("un estado que no es un código en mayúsculas es un 400")
	void estadoMalFormado() {
		String mensaje = "estado: Debe ser PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO o CANCELADO.";
		assert400(Map.of("estado", "XYZ"), mensaje);
		assert400(Map.of("estado", "pendiente"), mensaje);
		assert400(Map.of("estado", "EN CURSO"), mensaje);
	}

	@Test
	@DisplayName("una fecha que no es AAAA-MM-DD o no existe es un 400")
	void fechaMalFormada() {
		String mensaje = "fecha: Debe tener el formato AAAA-MM-DD.";
		assert400(Map.of("fecha", "ayer"), mensaje);
		assert400(Map.of("fecha", "2026-13-01"), mensaje);
		assert400(Map.of("fecha", "2026-02-30"), mensaje);
		assert400(Map.of("fecha", "28-10-2026"), mensaje);
		assert400(Map.of("fecha", "2026-1-5"), mensaje);
	}

	@Test
	@DisplayName("si fallan los dos filtros, una línea por cada uno")
	void ambosMalFormados() {
		assert400(Map.of("estado", "XYZ", "fecha", "ayer"),
				"estado: Debe ser PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO o CANCELADO.\n"
						+ "fecha: Debe tener el formato AAAA-MM-DD.");
	}
}
