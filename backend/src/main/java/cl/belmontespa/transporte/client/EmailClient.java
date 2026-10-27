package cl.belmontespa.transporte.client;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Revisión de cuentas (CU-17/RF26) por SMTP. El servidor se configura con
 * SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME y SPRING_MAIL_PASSWORD; sin
 * SPRING_MAIL_HOST solo se registra que no se envió. La contraseña temporal no viaja por aquí: la entrega el
 * Administrador (CU-13/RF21).
 */
@Slf4j
@Component
public class EmailClient {

	private final JavaMailSender mailSender;
	private final String remitente;
	private final Executor executor;

	@Autowired
	public EmailClient(ObjectProvider<JavaMailSender> mailSender, @Value("${app.mail.from}") String remitente) {
		this(mailSender.getIfAvailable(), remitente, Executors.newVirtualThreadPerTaskExecutor());
	}

	EmailClient(JavaMailSender mailSender, String remitente, Executor executor) {
		this.mailSender = mailSender;
		this.remitente = remitente;
		this.executor = executor;
	}

	public void enviarCuentaRevisada(String correo, String nombre, String razonSocial, boolean aprobado) {
		if (mailSender == null) {
			log.info("Correo no configurado (define SPRING_MAIL_HOST): no se envió el resultado de revisión a {}", correo);
			return;
		}
		SimpleMailMessage mensaje = new SimpleMailMessage();
		mensaje.setFrom(remitente);
		mensaje.setTo(correo);
		mensaje.setSubject("Belmonte SpA: tu cuenta fue " + (aprobado ? "aprobada" : "rechazada"));
		mensaje.setText((aprobado
				? "Hola %s: tu cuenta para %s fue aprobada. Ya puedes iniciar sesión con tu correo y contraseña."
				: "Hola %s: tu cuenta para %s no fue aprobada. Si crees que es un error, contacta a Belmonte SpA.")
				.formatted(nombre, razonSocial) + "\n\nBelmonte SpA\n");
		EnvioDiferido.ejecutar(executor, "enviar el resultado de revisión por correo", () -> mailSender.send(mensaje));
	}

	@PreDestroy
	void cerrar() {
		if (executor instanceof ExecutorService servicio) {
			servicio.close();
		}
	}
}
