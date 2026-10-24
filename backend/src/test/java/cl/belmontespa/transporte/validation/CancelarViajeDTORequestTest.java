package cl.belmontespa.transporte.validation;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.dto.request.CancelarViajeDTORequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CancelarViajeDTORequest: motivo de 5 a 200 caracteres y monto desde 0 (README, Validaciones)")
class CancelarViajeDTORequestTest {

	private static final Validator VALIDADOR = Validation.byDefaultProvider().configure()
			.messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();

	private static List<String> errores(CancelarViajeDTORequest dto) {
		return VALIDADOR.validate(dto).stream()
				.map((ConstraintViolation<CancelarViajeDTORequest> v) -> v.getPropertyPath() + ": " + v.getMessage())
				.sorted()
				.toList();
	}

	@Test
	@DisplayName("el motivo se recorta antes de validar")
	void recorta() {
		CancelarViajeDTORequest dto = new CancelarViajeDTORequest("  Sin conductor  ", null);

		assertThat(dto.motivo()).isEqualTo("Sin conductor");
		assertThat(errores(dto)).isEmpty();
	}

	@Test
	@DisplayName("el motivo tiene de 5 a 200 caracteres ya recortado")
	void largo() {
		assertThat(errores(new CancelarViajeDTORequest("1234", null)))
				.containsExactly("motivo: Debe tener entre 5 y 200 caracteres.");
		assertThat(errores(new CancelarViajeDTORequest("  1234  ", null)))
				.containsExactly("motivo: Debe tener entre 5 y 200 caracteres.");
		assertThat(errores(new CancelarViajeDTORequest("12345", null))).isEmpty();
		assertThat(errores(new CancelarViajeDTORequest("a".repeat(200), null))).isEmpty();
		assertThat(errores(new CancelarViajeDTORequest("a".repeat(201), null)))
				.containsExactly("motivo: Debe tener entre 5 y 200 caracteres.");
	}

	@Test
	@DisplayName("sin motivo, o solo con espacios, es obligatorio")
	void obligatorio() {
		assertThat(errores(new CancelarViajeDTORequest(null, null))).contains("motivo: Es obligatorio.");
		assertThat(errores(new CancelarViajeDTORequest("     ", null))).contains("motivo: Es obligatorio.");
	}

	@Test
	@DisplayName("el monto a reembolsar es opcional y no es negativo")
	void monto() {
		assertThat(errores(new CancelarViajeDTORequest("Sin conductor", null))).isEmpty();
		assertThat(errores(new CancelarViajeDTORequest("Sin conductor", 0))).isEmpty();
		assertThat(errores(new CancelarViajeDTORequest("Sin conductor", -1)))
				.containsExactly("montoReembolso: Debe ser mayor o igual que 0.");
	}
}
