package cl.belmontespa.transporte.client;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.util.Map;

public class FirebaseFcmSender implements FcmSender {

	private final FirebaseMessaging messaging;

	public FirebaseFcmSender(FirebaseMessaging messaging) {
		this.messaging = messaging;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void enviar(String tokenFcm, String titulo, String mensaje, Map<String, String> datos) {
		Message message = Message.builder()
				.setToken(tokenFcm)
				.setNotification(Notification.builder().setTitle(titulo).setBody(mensaje).build())
				.putAllData(datos)
				.build();
		try {
			messaging.send(message);
		} catch (FirebaseMessagingException e) {
			MessagingErrorCode codigo = e.getMessagingErrorCode();
			throw new FcmException(codigo == MessagingErrorCode.UNREGISTERED,
					"FCM rechazó el envío (" + e.getErrorCode() + (codigo == null ? "" : "/" + codigo) + "): "
							+ e.getMessage(), e);
		}
	}
}
