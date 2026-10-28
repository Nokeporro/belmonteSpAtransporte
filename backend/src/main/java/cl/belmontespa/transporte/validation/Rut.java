package cl.belmontespa.transporte.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

@Documented
@Constraint(validatedBy = Rut.Validador.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface Rut {

	String message() default "{validacion.rut}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validador implements ConstraintValidator<Rut, String> {

		private static final Pattern FORMATO = Pattern.compile("[0-9]{7,8}-[0-9K]");

		@Override
		public boolean isValid(String valor, ConstraintValidatorContext context) {
			if (valor == null) {
				return true;
			}
			if (!FORMATO.matcher(valor).matches()) {
				return false;
			}
			int guion = valor.length() - 2;
			return Reglas.digitoVerificador(valor.substring(0, guion)) == valor.charAt(guion + 1);
		}
	}
}
