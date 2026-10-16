package cl.belmontespa.transporte.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

@DisplayName("JwtProvider")
class JwtProviderTest {

	private static final String SECRETO = secreto("clave-de-prueba-de-32-bytes-minimo!");
	private static final String OTRO_SECRETO = secreto("otra-clave-de-prueba-de-32-bytes!!");

	private static String secreto(String texto) {
		return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("el token trae el correo como subject, el rol y la vigencia configurada")
	void generaYValida() {
		JwtProvider jwt = new JwtProvider(SECRETO, 900);

		Jwt decodificado = jwt.decoder().decode(jwt.generar("ana@test.cl", "SOLICITANTE"));

		assertThat(decodificado.getSubject()).isEqualTo("ana@test.cl");
		assertThat(decodificado.<String>getClaim("rol")).isEqualTo("SOLICITANTE");
		assertThat(decodificado.getExpiresAt().getEpochSecond() - decodificado.getIssuedAt().getEpochSecond())
				.isEqualTo(900);
		assertThat(jwt.getExpirationSeconds()).isEqualTo(900);
	}

	@Test
	@DisplayName("un token firmado con otra clave se rechaza")
	void rechazaOtraClave() {
		String ajeno = new JwtProvider(OTRO_SECRETO, 900).generar("ana@test.cl", "ADMINISTRADOR");

		assertThatThrownBy(() -> new JwtProvider(SECRETO, 900).decoder().decode(ajeno))
				.isInstanceOf(JwtException.class);
	}

	@Test
	@DisplayName("cambiar el rol dentro del token sin volver a firmarlo lo invalida")
	void rechazaRolAlterado() {
		JwtProvider jwt = new JwtProvider(SECRETO, 900);
		String[] partes = jwt.generar("ana@test.cl", "SOLICITANTE").split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
				.replace("SOLICITANTE", "ADMINISTRADOR");
		String alterado = partes[0] + "."
				+ Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
				+ "." + partes[2];

		assertThatThrownBy(() -> jwt.decoder().decode(alterado)).isInstanceOf(JwtException.class);
	}

	@Test
	@DisplayName("un texto que no es un JWT se rechaza")
	void rechazaBasura() {
		assertThatThrownBy(() -> new JwtProvider(SECRETO, 900).decoder().decode("no-es-un-jwt"))
				.isInstanceOf(JwtException.class);
	}

	@Test
	@DisplayName("la vigencia debe ser positiva")
	void vigenciaInvalida() {
		assertThatThrownBy(() -> new JwtProvider(SECRETO, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new JwtProvider(SECRETO, -5)).isInstanceOf(IllegalArgumentException.class);
	}
}
