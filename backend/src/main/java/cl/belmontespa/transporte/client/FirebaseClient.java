package cl.belmontespa.transporte.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Push por FCM (RF12). Implementación provisoria: solo registra la notificación;
// falta conectar el SDK de Firebase Admin.
@Slf4j
@Component
public class FirebaseClient {

	public void notificar(String tokenFcm, String titulo, String mensaje) {
		if (tokenFcm == null || tokenFcm.isBlank()) {
			return;
		}
		log.info("Push no configurado; se omitió '{}': {}", titulo, mensaje);
	}
}
