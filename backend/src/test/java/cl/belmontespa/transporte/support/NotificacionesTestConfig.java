package cl.belmontespa.transporte.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class NotificacionesTestConfig {

	@Bean
	FcmGrabador fcmGrabador() {
		return new FcmGrabador();
	}
}
