package cl.belmontespa.transporte.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Deja el esquema de pruebas en V1–V3 limpias al arrancar el contexto: así cada ejecución
 * parte del administrador sembrado recién creado y sin datos de corridas anteriores.
 */
@TestConfiguration
public class ReiniciarEsquemaConfig {

	@Bean
	FlywayMigrationStrategy reiniciarEsquema(@Value("${spring.datasource.url}") String url) {
		return flyway -> {
			exigirEsquemaDePruebas(url);
			flyway.clean();
			flyway.migrate();
		};
	}

	// Guarda: Flyway.clean() borra todo el esquema, así que nunca se ejecuta contra otra base.
	static void exigirEsquemaDePruebas(String url) {
		String sinParametros = url == null ? "" : url.split("\\?", 2)[0];
		if (!sinParametros.endsWith("_test")) {
			throw new IllegalStateException(
					"Los tests solo pueden limpiar un esquema cuyo nombre termina en _test; URL: " + url);
		}
	}
}
