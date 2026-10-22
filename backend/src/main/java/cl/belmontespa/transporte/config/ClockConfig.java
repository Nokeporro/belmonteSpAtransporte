package cl.belmontespa.transporte.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// "Ahora" sale de este Clock, en hora de Chile, nunca de la zona de la JVM: es la misma zona de la
// sesión de la base (spring.datasource.hikari.connection-init-sql), así que lo que escribe la app
// y lo que escriben los triggers (CURRENT_TIMESTAMP) se pueden comparar.
@Configuration
public class ClockConfig {

	public static final ZoneId ZONA = ZoneId.of("America/Santiago");

	@Bean
	public Clock clock() {
		return Clock.system(ZONA);
	}
}
