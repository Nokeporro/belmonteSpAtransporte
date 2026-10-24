package cl.belmontespa.transporte.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@DisplayName("EmailClient")
class EmailClientTest {

	private static final String PASSWORD = "Abc123xyzQ98";

	@RegisterExtension
	GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort());

	private ListAppender<ILoggingEvent> logs;

	@BeforeEach
	void capturarLogs() {
		Logger raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logs = new ListAppender<>();
		logs.start();
		raiz.addAppender(logs);
	}

	@AfterEach
	void soltarLogs() {
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(logs);
	}

	private JavaMailSenderImpl servidor(int puerto) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost("localhost");
		sender.setPort(puerto);
		Properties props = sender.getJavaMailProperties();
		props.put("mail.smtp.connectiontimeout", "1000");
		props.put("mail.smtp.timeout", "1000");
		return sender;
	}

	private EmailClient cliente(JavaMailSenderImpl sender) {
		return new EmailClient(sender, "no-reply@belmontespa.cl", Runnable::run);
	}

	private List<String> mensajesDeLog() {
		return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
	}

	@Test
	@DisplayName("envía la contraseña y su vigencia al destinatario, desde el remitente configurado")
	void envia() throws Exception {
		cliente(servidor(greenMail.getSmtp().getPort())).enviarPasswordTemporal("ana@test.cl", "Ana", PASSWORD, LocalDateTime.of(2026, 10, 24, 12, 30));

		MimeMessage[] recibidos = greenMail.getReceivedMessages();
		assertThat(recibidos).hasSize(1);
		assertThat(recibidos[0].getAllRecipients()[0].toString()).isEqualTo("ana@test.cl");
		assertThat(recibidos[0].getFrom()[0].toString()).isEqualTo("no-reply@belmontespa.cl");
		assertThat(recibidos[0].getSubject()).isEqualTo("Belmonte SpA: tu contraseña temporal");
		assertThat(((String) recibidos[0].getContent()).replace("\r\n", "\n").stripTrailing()).isEqualTo(
				"Hola Ana: tu contraseña temporal es " + PASSWORD + " y vence el 24-10-2026 12:30:00. Úsala para iniciar sesión; la app te pedirá crear una nueva. Si no la pediste, ignora este correo: tu contraseña actual sigue funcionando.\n\nBelmonte SpA");
	}

	@Test
	@DisplayName("sin servidor SMTP configurado no falla ni envía")
	void sinServidor() {
		assertThatCode(() -> new EmailClient(null, "no-reply@belmontespa.cl", Runnable::run)
				.enviarPasswordTemporal("ana@test.cl", "Ana", PASSWORD, LocalDateTime.of(2026, 10, 24, 12, 30))).doesNotThrowAnyException();

		assertThat(greenMail.getReceivedMessages()).isEmpty();
		assertThat(mensajesDeLog()).anyMatch(m -> m.contains("Correo no configurado"));
	}

	@Test
	@DisplayName("si el servidor SMTP no responde, la excepción no llega al llamador")
	void servidorCaido() throws Exception {
		int puertoCerrado;
		try (ServerSocket libre = new ServerSocket(0)) {
			puertoCerrado = libre.getLocalPort();
		}

		assertThatCode(() -> cliente(servidor(puertoCerrado)).enviarPasswordTemporal("ana@test.cl", "Ana", PASSWORD, LocalDateTime.of(2026, 10, 24, 12, 30)))
				.doesNotThrowAnyException();

		assertThat(mensajesDeLog()).anyMatch(m -> m.contains("No se pudo enviar la contraseña temporal"));
	}

	@Test
	@DisplayName("la contraseña no aparece en los logs, ni al enviar, ni sin servidor, ni si falla")
	void noRegistraLaPassword() throws Exception {
		cliente(servidor(greenMail.getSmtp().getPort())).enviarPasswordTemporal("ana@test.cl", "Ana", PASSWORD, LocalDateTime.of(2026, 10, 24, 12, 30));
		new EmailClient(null, "no-reply@belmontespa.cl", Runnable::run)
				.enviarPasswordTemporal("ana@test.cl", "Ana", PASSWORD, LocalDateTime.of(2026, 10, 24, 12, 30));
		int puertoCerrado;
		try (ServerSocket libre = new ServerSocket(0)) {
			puertoCerrado = libre.getLocalPort();
		}
		cliente(servidor(puertoCerrado)).enviarPasswordTemporal("ana@test.cl", "Ana", PASSWORD, LocalDateTime.of(2026, 10, 24, 12, 30));

		assertThat(mensajesDeLog()).isNotEmpty().noneMatch(m -> m.contains(PASSWORD));
		assertThat(logs.list).noneMatch(e -> e.getThrowableProxy() != null
				&& e.getThrowableProxy().getMessage() != null && e.getThrowableProxy().getMessage().contains(PASSWORD));
	}
}
