package cl.belmontespa.transporte.validation;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.dto.request.TokenFcmDTORequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenFcmDTORequest: token obligatorio de hasta 255 caracteres")
class TokenFcmDTORequestTest {
	private static final Validator VALIDADOR = Validation.byDefaultProvider().configure()
			.messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();

	private List<String> errores(String token) {
		return VALIDADOR.validate(new TokenFcmDTORequest(token)).stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
	}

	@Test
	void rechazaTokenAusenteVacioOSoloEspacios() {
		assertThat(errores(null)).containsExactly("tokenFcm: Es obligatorio.");
		assertThat(errores("")).containsExactly("tokenFcm: Es obligatorio.");
		assertThat(errores(" \t\n ")).containsExactly("tokenFcm: Es obligatorio.");
	}

	@Test
	void aceptaHasta255CaracteresYRechaza256() {
		assertThat(errores("a")).isEmpty();
		assertThat(errores("a".repeat(255))).isEmpty();
		assertThat(errores("a".repeat(256))).hasSize(1)
				.allMatch(error -> error.startsWith("tokenFcm:"));
	}

	@Test
	void conservaElTokenSinCambiarSuContenido() {
		String token = "Dispositivo:Token_A-b.123";
		assertThat(new TokenFcmDTORequest(token).tokenFcm()).isEqualTo(token);
		assertThat(errores(token)).isEmpty();
	}
}
