package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.client.FirebaseClient;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import java.util.Map;

final class AvisosAdministracion {
	private AvisosAdministracion() {
	}

	static void notificar(AdministradorRepository administradores, FirebaseClient firebaseClient,
			String titulo, String mensaje, Map<String, String> datos) {
		administradores.findAll().forEach(administrador -> {
			String token = administrador.getUsuario().getTokenFcm();
			if (token != null && !token.isBlank()) {
				firebaseClient.notificar(token, titulo, mensaje, datos);
			}
		});
	}
}
