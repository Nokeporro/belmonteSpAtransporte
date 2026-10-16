package cl.belmontespa.transporte.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Sustituye el envío real por Firebase: sin credenciales, FirebaseClient usa este grabador. */
@TestConfiguration
public class NotificacionesTestConfig {

	@Bean
	FcmGrabador fcmGrabador() {
		return new FcmGrabador();
	}
}
