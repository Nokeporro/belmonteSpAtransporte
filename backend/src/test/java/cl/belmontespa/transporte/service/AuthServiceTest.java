package cl.belmontespa.transporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import cl.belmontespa.transporte.config.JwtProvider;
import cl.belmontespa.transporte.dto.request.LoginDTORequest;
import cl.belmontespa.transporte.model.Usuario;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import cl.belmontespa.transporte.repository.ConductorRepository;
import cl.belmontespa.transporte.repository.SolicitanteRepository;
import cl.belmontespa.transporte.repository.UsuarioRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("AuthService: registro y reemplazo del token FCM")
class AuthServiceTest {
	private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
	private final AdministradorRepository administradores = mock(AdministradorRepository.class);
	private final PasswordEncoder encoder = mock(PasswordEncoder.class);
	private final JwtProvider jwt = mock(JwtProvider.class);
	private final AuthService servicio = new AuthService(usuarios, administradores,
			mock(ConductorRepository.class), mock(SolicitanteRepository.class), encoder, jwt, Clock.systemUTC(), 24);

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setIdUsuario(10L);
		usuario.setCorreo("ana@test.cl");
		usuario.setPasswordHash("hash");
		usuario.setTokenFcm("token-anterior");
		return usuario;
	}

	@Test
	void reemplazaElTokenYLoQuitaDeOtrasCuentas() {
		Usuario usuario = usuario();
		when(usuarios.bloquearPorCorreo(usuario.getCorreo())).thenReturn(Optional.of(usuario));

		servicio.registrarTokenFcm(usuario.getCorreo(), "token-nuevo");

		assertThat(usuario.getTokenFcm()).isEqualTo("token-nuevo");
		var orden = inOrder(usuarios);
		orden.verify(usuarios).bloquearPorCorreo(usuario.getCorreo());
		orden.verify(usuarios).quitarTokenFcm("token-nuevo", usuario.getIdUsuario());
	}

	@Test
	void repetirElRegistroConservaElToken() {
		Usuario usuario = usuario();
		when(usuarios.bloquearPorCorreo(usuario.getCorreo())).thenReturn(Optional.of(usuario));

		servicio.registrarTokenFcm(usuario.getCorreo(), "token-nuevo");
		servicio.registrarTokenFcm(usuario.getCorreo(), "token-nuevo");

		assertThat(usuario.getTokenFcm()).isEqualTo("token-nuevo");
		verify(usuarios, times(2)).quitarTokenFcm("token-nuevo", usuario.getIdUsuario());
	}

	@Test
	void unaCuentaInexistenteNoPuedeApropiarseDeUnToken() {
		when(usuarios.bloquearPorCorreo("nadie@test.cl")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.registrarTokenFcm("nadie@test.cl", "token-nuevo"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(usuarios, never()).quitarTokenFcm(anyString(), anyLong());
	}

	@Test
	void elLoginAplicaLaMismaReglaDeTokenUnico() {
		Usuario usuario = usuario();
		when(usuarios.bloquearPorCorreo(usuario.getCorreo())).thenReturn(Optional.of(usuario));
		when(encoder.matches("Clave12345", "hash")).thenReturn(true);
		when(administradores.existsById(usuario.getIdUsuario())).thenReturn(true);

		servicio.login(new LoginDTORequest(usuario.getCorreo(), "Clave12345", "token-login"));

		assertThat(usuario.getTokenFcm()).isEqualTo("token-login");
		verify(usuarios).quitarTokenFcm("token-login", usuario.getIdUsuario());
	}

	@Test
	void elLoginSinTokenConservaElExistente() {
		Usuario usuario = usuario();
		when(usuarios.bloquearPorCorreo(usuario.getCorreo())).thenReturn(Optional.of(usuario));
		when(encoder.matches("Clave12345", "hash")).thenReturn(true);
		when(administradores.existsById(usuario.getIdUsuario())).thenReturn(true);

		servicio.login(new LoginDTORequest(usuario.getCorreo(), "Clave12345", null));

		assertThat(usuario.getTokenFcm()).isEqualTo("token-anterior");
		verify(usuarios, never()).quitarTokenFcm(anyString(), anyLong());
	}
}
