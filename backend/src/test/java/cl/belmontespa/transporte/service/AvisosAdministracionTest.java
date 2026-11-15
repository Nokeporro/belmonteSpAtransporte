package cl.belmontespa.transporte.service;

import static org.mockito.Mockito.*;

import cl.belmontespa.transporte.client.FirebaseClient;
import cl.belmontespa.transporte.model.Administrador;
import cl.belmontespa.transporte.model.Usuario;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AvisosAdministracionTest {
	private final AdministradorRepository administradores = mock(AdministradorRepository.class);
	private final FirebaseClient firebase = mock(FirebaseClient.class);

	private Administrador administrador(String token) {
		Usuario usuario = new Usuario();
		usuario.setTokenFcm(token);
		Administrador administrador = new Administrador();
		administrador.setUsuario(usuario);
		return administrador;
	}

	@Test
	void notificaATodosLosAdministradoresConTokenYOmiteLosNoRegistrados() {
		when(administradores.findAll()).thenReturn(List.of(administrador("token-uno"), administrador(null),
				administrador(""), administrador("  "), administrador("token-dos")));
		Map<String, String> datos = Map.of("tipo", "CUENTAS");

		AvisosAdministracion.notificar(administradores, firebase,
				"Cuenta por aprobar", "Hay una cuenta nueva por revisar", datos);

		verify(firebase).notificar("token-uno", "Cuenta por aprobar", "Hay una cuenta nueva por revisar", datos);
		verify(firebase).notificar("token-dos", "Cuenta por aprobar", "Hay una cuenta nueva por revisar", datos);
		verifyNoMoreInteractions(firebase);
	}

	@Test
	void sinAdministradoresNoEnviaNotificaciones() {
		when(administradores.findAll()).thenReturn(List.of());

		AvisosAdministracion.notificar(administradores, firebase, "Aviso", "Mensaje", Map.of("tipo", "CUENTAS"));

		verifyNoInteractions(firebase);
	}
}
