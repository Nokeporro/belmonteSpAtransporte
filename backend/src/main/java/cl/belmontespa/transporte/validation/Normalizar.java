package cl.belmontespa.transporte.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Normalizar {

	private static final Pattern SEPARADORES_TELEFONO = Pattern.compile("[\\s\\-()]");
	private static final Pattern NUEVE_DIGITOS = Pattern.compile("[0-9]{9}");
	private static final Pattern PUNTOS_Y_ESPACIOS = Pattern.compile("[.\\s]");
	private static final Pattern ESPACIOS_Y_GUIONES = Pattern.compile("[\\s-]");
	private static final Pattern ESPACIOS = Pattern.compile("\\s");

	private Normalizar() {
	}

	public static String texto(String valor) {
		return valor == null ? null : valor.strip();
	}

	public static String correo(String valor) {
		return valor == null ? null : valor.strip().toLowerCase(Locale.ROOT);
	}

	public static String telefono(String valor) {
		if (valor == null) {
			return null;
		}
		String limpio = SEPARADORES_TELEFONO.matcher(valor).replaceAll("");
		if (limpio.isEmpty()) {
			return null;
		}
		return NUEVE_DIGITOS.matcher(limpio).matches() ? "+56" + limpio : limpio;
	}

	public static String patente(String valor) {
		return valor == null ? null : ESPACIOS_Y_GUIONES.matcher(valor).replaceAll("").toUpperCase(Locale.ROOT);
	}

	public static String codigoAbordaje(String valor) {
		return valor == null ? null : ESPACIOS.matcher(valor).replaceAll("").toUpperCase(Locale.ROOT);
	}

	public static String rut(String valor) {
		if (valor == null) {
			return null;
		}
		String limpio = PUNTOS_Y_ESPACIOS.matcher(valor).replaceAll("").toUpperCase(Locale.ROOT);
		int largo = limpio.length();
		if (largo > 1 && limpio.indexOf('-') < 0) {
			limpio = limpio.substring(0, largo - 1) + "-" + limpio.substring(largo - 1);
		}
		return limpio;
	}

	public static String numeroVuelo(String valor) {
		if (valor == null) {
			return null;
		}
		String limpio = ESPACIOS.matcher(valor).replaceAll("").toUpperCase(Locale.ROOT);
		return limpio.isEmpty() ? null : limpio;
	}
}
