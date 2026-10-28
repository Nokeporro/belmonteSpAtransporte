package cl.belmontespa.transporte.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

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

	static void exigirEsquemaDePruebas(String url) {
		String sinParametros = url == null ? "" : url.split("\\?", 2)[0];
		if (!sinParametros.endsWith("_test")) {
			throw new IllegalStateException(
					"Los tests solo pueden limpiar un esquema cuyo nombre termina en _test; URL: " + url);
		}
	}
}
