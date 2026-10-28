package cl.belmontespa.transporte.validation;

import java.util.Comparator;
import java.util.regex.Pattern;

public final class Reglas {

	public static final String CORREO = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
	public static final String TELEFONO = "^\\+56[0-9]{9}$";
	public static final String PATENTE = "^([A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2})$";
	public static final String CODIGO_ABORDAJE = "^[0-9A-F]{16}$";
	public static final String NUMERO_VUELO = "^[A-Z0-9]{2}[0-9]{1,4}[A-Z]?$";
	public static final String FOLIO = "^TR-[1-9][0-9]{3,16}$";
	public static final String HASH_PAGO = "^[0-9a-f]{32}$";
	public static final String DNI_HASH = "^[A-Za-z0-9_-]{43}$";

	public static final String MAXIMO = "{validacion.maximo}";

	public static final Comparator<String> FOLIO_POR_NUMERO = Comparator.comparingLong(Reglas::numeroDeFolio);

	private static final Pattern FOLIO_RE = Pattern.compile(FOLIO);
	private static final Pattern HASH_PAGO_RE = Pattern.compile(HASH_PAGO);
	private static final Pattern DNI_HASH_RE = Pattern.compile(DNI_HASH);

	private Reglas() {
	}

	public static long numeroDeFolio(String folio) {
		return Long.parseLong(folio.substring("TR-".length()));
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
