package cl.belmontespa.transporte.validation;

import java.util.regex.Pattern;

// Reglas de la tabla "Validaciones" del README, compartidas por los DTO de entrada y por las
// claves de ruta. Se aplican sobre el valor ya normalizado (ver Normalizar).
public final class Reglas {

	public static final String CORREO = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
	public static final String TELEFONO = "^\\+56[0-9]{9}$";
	public static final String PATENTE = "^([A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2})$";
	public static final String NUMERO_VUELO = "^[A-Z0-9]{2}[0-9]{1,4}[A-Z]?$";
	public static final String FOLIO = "^TR-[1-9][0-9]{3,16}$";
	public static final String HASH_PAGO = "^[0-9a-f]{32}$";
	public static final String DNI_HASH = "^[A-Za-z0-9_-]{43}$";

	// Mensaje de @Size cuando la regla es solo un máximo: "Debe tener como máximo 150 caracteres."
	public static final String MAXIMO = "{validacion.maximo}";

	private static final Pattern FOLIO_RE = Pattern.compile(FOLIO);
	private static final Pattern HASH_PAGO_RE = Pattern.compile(HASH_PAGO);
	private static final Pattern DNI_HASH_RE = Pattern.compile(DNI_HASH);

	private Reglas() {
	}

	public static boolean esFolio(String valor) {
		return valor != null && FOLIO_RE.matcher(valor).matches();
	}

	public static boolean esHashPago(String valor) {
		return valor != null && HASH_PAGO_RE.matcher(valor).matches();
	}

	public static boolean esDniHash(String valor) {
		return valor != null && DNI_HASH_RE.matcher(valor).matches();
	}

	// Módulo 11: factores 2, 3, 4, 5, 6, 7, 2… de derecha a izquierda; 11 − (suma mod 11),
	// con 11 → '0' y 10 → 'K'. `cuerpo` son solo los dígitos, sin el dígito verificador.
	public static char digitoVerificador(String cuerpo) {
		int suma = 0;
		int factor = 2;
		for (int i = cuerpo.length() - 1; i >= 0; i--) {
			suma += (cuerpo.charAt(i) - '0') * factor;
			factor = factor == 7 ? 2 : factor + 1;
		}
		int digito = 11 - suma % 11;
		return digito == 11 ? '0' : digito == 10 ? 'K' : (char) ('0' + digito);
	}
}
