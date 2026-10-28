package cl.belmontespa.transporte.support;

import cl.belmontespa.transporte.client.FcmSender;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class FcmGrabador implements FcmSender {

	public record Envio(String token, String titulo, String mensaje, Map<String, String> datos) {
	}

	private final List<Envio> envios = new CopyOnWriteArrayList<>();

	@Override
	public void enviar(String tokenFcm, String titulo, String mensaje, Map<String, String> datos) {
		envios.add(new Envio(tokenFcm, titulo, mensaje, datos));
	}

	public List<Envio> envios() {
		return List.copyOf(envios);
	}

	public List<Envio> para(String tokenFcm) {
		return envios().stream().filter(envio -> envio.token().equals(tokenFcm)).toList();
	}

	public void limpiar() {
		envios.clear();
	}
}
