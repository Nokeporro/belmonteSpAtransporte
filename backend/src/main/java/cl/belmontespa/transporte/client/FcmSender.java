package cl.belmontespa.transporte.client;

import java.util.Map;

/** Envío de un mensaje a un dispositivo por Firebase Cloud Messaging. */
public interface FcmSender {

	void enviar(String tokenFcm, String titulo, String mensaje, Map<String, String> datos);

	/** Falla del envío; {@code tokenNoRegistrado} indica que el dispositivo ya no existe. */
	class FcmException extends RuntimeException {

		private final boolean tokenNoRegistrado;

		public FcmException(boolean tokenNoRegistrado, String mensaje, Throwable causa) {
			super(mensaje, causa);
			this.tokenNoRegistrado = tokenNoRegistrado;
		}

		public boolean isTokenNoRegistrado() {
			return tokenNoRegistrado;
		}
	}
}
