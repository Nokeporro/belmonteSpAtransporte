package cl.belmontespa.transporte.validation;

import java.util.Locale;
import java.util.regex.Pattern;

// Normalización previa a la validación (README, "Validaciones"): el backend guarda el valor
// normalizado. Los DTO la llaman desde su constructor compacto, que corre al deserializar y
// antes de @Valid. Todos tratan null como "ausente" y no lo tocan.
public final class Normalizar {

	private static final Pattern SEPARADORES_TELEFONO = Pattern.compile("[\\s\\-()]");
	private static final Pattern NUEVE_DIGITOS = Pattern.compile("[0-9]{9}");
	private static final Pattern PUNTOS_Y_ESPACIOS = Pattern.compile("[.\\s]");
	private static final Pattern ESPACIOS_Y_GUIONES = Pattern.compile("[\\s-]");
	private static final Pattern ESPACIOS = Pattern.compile("\\s");

	private Normalizar() {
	}

	// nombre, razonSocial, direccion, comuna, region.
	public static String texto(String valor) {
		return valor == null ? null : valor.strip();
	}

	public static String correo(String valor) {
		return valor == null ? null : valor.strip().toLowerCase(Locale.ROOT);
	}

	// Sin espacios, guiones ni paréntesis; 9 dígitos chilenos pasan a "+56…". Vacío es ausente.
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

	// Código del QR de abordaje: en mayúsculas y sin espacios ("7f3a 9c21 …" → "7F3A9C21…").
	public static String codigoAbordaje(String valor) {
		return valor == null ? null : ESPACIOS.matcher(valor).replaceAll("").toUpperCase(Locale.ROOT);
	}

	// Sin puntos, con guion antes del dígito verificador y K mayúscula: 76.543.210-3 → 76543210-3.
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

	// En mayúsculas y sin espacios: "la 1502" → "LA1502". Vacío es ausente (el campo es opcional).
	public static String numeroVuelo(String valor) {
		if (valor == null) {
			return null;
		}
		String limpio = ESPACIOS.matcher(valor).replaceAll("").toUpperCase(Locale.ROOT);
		return limpio.isEmpty() ? null : limpio;
	}
}
