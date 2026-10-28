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
import java.nio.charset.StandardCharsets;

@Documented
@Constraint(validatedBy = Contrasena.Validador.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface Contrasena {

	String message() default "{validacion.contrasena.largo}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validador implements ConstraintValidator<Contrasena, String> {

		static final int MINIMO = 8;
		static final int MAXIMO = 72;

		@Override
		public boolean isValid(String valor, ConstraintValidatorContext context) {
			if (valor == null) {
				return true;
			}
			int caracteres = valor.codePointCount(0, valor.length());
			if (caracteres < MINIMO || caracteres > MAXIMO) {
				return rechazar(context, "{validacion.contrasena.largo}");
			}
			if (valor.getBytes(StandardCharsets.UTF_8).length > MAXIMO) {
				return rechazar(context, "{validacion.contrasena.bytes}");
			}
			boolean conLetra = valor.codePoints().anyMatch(Character::isLetter);
			boolean conDigito = valor.codePoints().anyMatch(Character::isDigit);
			if (!conLetra || !conDigito) {
				return rechazar(context, "{validacion.contrasena.composicion}");
			}
			return true;
		}

		private static boolean rechazar(ConstraintValidatorContext context, String mensaje) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(mensaje).addConstraintViolation();
			return false;
		}
	}
}
