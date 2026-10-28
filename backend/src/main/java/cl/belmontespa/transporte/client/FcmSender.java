package cl.belmontespa.transporte.client;

import java.util.Map;

public interface FcmSender {

	void enviar(String tokenFcm, String titulo, String mensaje, Map<String, String> datos);

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
