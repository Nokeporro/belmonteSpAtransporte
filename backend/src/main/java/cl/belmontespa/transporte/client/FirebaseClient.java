package cl.belmontespa.transporte.client;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FirebaseClient {

	private final FcmSender sender;
	private final Executor executor;

	@Autowired
	public FirebaseClient(ObjectProvider<FcmSender> sender) {
		this(sender.getIfAvailable(), Executors.newVirtualThreadPerTaskExecutor());
	}

	FirebaseClient(FcmSender sender, Executor executor) {
		this.sender = sender;
		this.executor = executor;
	}

	public void notificar(String tokenFcm, String titulo, String mensaje, Map<String, String> datos) {
		if (tokenFcm == null || tokenFcm.isBlank()) {
			return;
		}
		if (sender == null) {
			log.info("Push no configurado (define FIREBASE_CREDENTIALS_FILE); se omitió '{}'", titulo);
			return;
		}
		EnvioDiferido.ejecutar(executor, "enviar el push '" + titulo + "'", () -> {
			try {
				sender.enviar(tokenFcm, titulo, mensaje, datos);
			} catch (FcmSender.FcmException e) {
				if (e.isTokenNoRegistrado()) {
					log.warn("El token FCM ya no está registrado; el dispositivo no recibió '{}'", titulo);
				} else {
					throw e;
				}
			}
		});
	}

	@PreDestroy
	void cerrar() {
		if (executor instanceof ExecutorService servicio) {
			servicio.close();
		}
	}
}
