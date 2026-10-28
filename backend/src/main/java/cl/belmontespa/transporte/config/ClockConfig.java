package cl.belmontespa.transporte.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	public static final ZoneId ZONA = ZoneId.of("America/Santiago");

	@Bean
	public Clock clock() {
		return Clock.system(ZONA);
	}
}
