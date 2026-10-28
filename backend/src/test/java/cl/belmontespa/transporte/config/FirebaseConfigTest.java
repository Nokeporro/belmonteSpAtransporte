package cl.belmontespa.transporte.config;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.client.FcmSender;
import cl.belmontespa.transporte.client.FirebaseFcmSender;
import com.google.firebase.FirebaseApp;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;

@DisplayName("FirebaseConfig: FIREBASE_CREDENTIALS_FILE")
class FirebaseConfigTest {

	@TempDir
	Path carpeta;

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(FirebaseConfig.class);

	@AfterEach
	void liberarFirebase() {
		FirebaseApp.getApps().forEach(FirebaseApp::delete);
	}

	private Path cuentaDeServicio() throws Exception {
		KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
		generador.initialize(2048);
		String pem = "-----BEGIN PRIVATE KEY-----\n"
				+ Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
						.encodeToString(generador.generateKeyPair().getPrivate().getEncoded())
				+ "\n-----END PRIVATE KEY-----\n";
		String json = """
				{"type":"service_account","project_id":"belmonte-test","private_key_id":"0123456789abcdef",
				"private_key":"%s","client_email":"test@belmonte-test.iam.gserviceaccount.com",
				"client_id":"1","token_uri":"https://oauth2.googleapis.com/token"}"""
				.formatted(pem.replace("\n", "\\n"));
		return Files.writeString(carpeta.resolve("cuenta.json"), json);
	}

	@Test
	@DisplayName("sin la variable no hay envío por Firebase y la app arranca igual")
	void sinVariable() {
		runner.run(contexto -> {
			assertThat(contexto).hasNotFailed();
			assertThat(contexto).doesNotHaveBean(FcmSender.class);
		});
	}

	@Test
	@DisplayName("con una cuenta de servicio válida se crea el envío por Firebase")
	void conCuentaValida() throws Exception {
		Path cuenta = cuentaDeServicio();
		runner.withInitializer(contexto -> TestPropertyValues.of("FIREBASE_CREDENTIALS_FILE=" + cuenta)
				.applyTo(contexto)).run(contexto -> {
					assertThat(contexto).hasNotFailed();
					assertThat(contexto).getBean(FcmSender.class).isInstanceOf(FirebaseFcmSender.class);
				});
	}

	@Test
	@DisplayName("un archivo inexistente impide arrancar y el error nombra la ruta")
	void archivoInexistente() {
		runner.withInitializer(contexto -> TestPropertyValues
				.of("FIREBASE_CREDENTIALS_FILE=" + carpeta.resolve("no-existe.json")).applyTo(contexto))
				.run(contexto -> {
					assertThat(contexto).hasFailed();
					assertThat(contexto.getStartupFailure()).rootCause().isInstanceOf(java.io.IOException.class);
					assertThat(contexto.getStartupFailure()).hasStackTraceContaining(
							"No se pudieron cargar las credenciales de Firebase").hasStackTraceContaining("no-existe.json");
				});
	}

	@Test
	@DisplayName("un archivo que no es una cuenta de servicio impide arrancar")
	void archivoInvalido() throws Exception {
		Path basura = Files.writeString(carpeta.resolve("basura.json"), "{}");
		runner.withInitializer(contexto -> TestPropertyValues.of("FIREBASE_CREDENTIALS_FILE=" + basura)
				.applyTo(contexto)).run(contexto -> {
					assertThat(contexto).hasFailed();
					assertThat(contexto.getStartupFailure())
							.hasStackTraceContaining("No se pudieron cargar las credenciales de Firebase");
				});
	}
}
