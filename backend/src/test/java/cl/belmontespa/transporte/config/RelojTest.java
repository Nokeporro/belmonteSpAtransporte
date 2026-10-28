package cl.belmontespa.transporte.config;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.validation.Reglas;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Reloj: hora de Chile y folios por número (README, Fechas y Consultas)")
class RelojTest {

	@Test
	@DisplayName("el Clock del backend va en America/Santiago y sigue al reloj del sistema")
	void zona() {
		Clock clock = new ClockConfig().clock();
		assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Santiago"));
		long antes = System.currentTimeMillis();
		assertThat(clock.millis()).isBetween(antes - 1_000, antes + 1_000);
	}

	@Test
	@DisplayName("ninguna clase de main pide la hora sin pasar por el Clock")
	void sinHoraDelSistema() throws IOException {
		Pattern sinReloj = Pattern.compile(
				"\\b(LocalDateTime|LocalDate|LocalTime|ZonedDateTime|OffsetDateTime)\\.now\\(\\s*\\)");
		try (Stream<Path> archivos = Files.walk(Path.of("src/main/java"))) {
			List<String> infractores = archivos
					.filter(archivo -> archivo.toString().endsWith(".java"))
					.filter(archivo -> sinReloj.matcher(leer(archivo)).find())
					.map(Path::toString)
					.toList();
			assertThat(infractores).as("archivos que usan now() sin Clock").isEmpty();
		}
	}

	@Test
	@DisplayName("el folio se ordena por su número: TR-9999 antes que TR-10000")
	void folioPorNumero() {
		List<String> folios = new ArrayList<>(List.of("TR-10000", "TR-1001", "TR-9999", "TR-100000"));
		folios.sort(Reglas.FOLIO_POR_NUMERO);
		assertThat(folios).containsExactly("TR-1001", "TR-9999", "TR-10000", "TR-100000");

		assertThat(folios.stream().sorted().toList()).containsExactly("TR-10000", "TR-100000", "TR-1001", "TR-9999");
	}

	@Test
	@DisplayName("el número de un folio de 17 dígitos cabe en un long")
	void numeroDeFolio() {
		assertThat(Reglas.numeroDeFolio("TR-1001")).isEqualTo(1001L);
		assertThat(Reglas.numeroDeFolio("TR-99999999999999999")).isEqualTo(99_999_999_999_999_999L);
	}

	private static String leer(Path archivo) {
		try {
			return Files.readString(archivo);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
