package cl.belmontespa.transporte.config;

import cl.belmontespa.transporte.client.FcmSender;
import cl.belmontespa.transporte.client.FirebaseFcmSender;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

	@Bean
	@ConditionalOnExpression("!'${app.firebase.credentials-file:}'.isBlank()")
	FcmSender fcmSender(@Value("${app.firebase.credentials-file}") String archivo) {
		try (InputStream credenciales = Files.newInputStream(Path.of(archivo))) {
			FirebaseApp app = FirebaseApp.getApps().isEmpty()
					? FirebaseApp.initializeApp(FirebaseOptions.builder()
							.setCredentials(GoogleCredentials.fromStream(credenciales)).build())
					: FirebaseApp.getInstance();
			return new FirebaseFcmSender(FirebaseMessaging.getInstance(app));
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException(
					"No se pudieron cargar las credenciales de Firebase desde '" + archivo + "': " + e.getMessage(), e);
		}
	}
}
