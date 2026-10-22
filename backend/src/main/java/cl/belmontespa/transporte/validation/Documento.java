package cl.belmontespa.transporte.validation;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Compartido por el alta del pasajero y el abordaje; nunca persiste el documento.
public final class Documento {
    private Documento() {}

    public static String normalizar(String tipo, String dni) {
        String normalizado = dni == null ? "" : dni.replaceAll("[.\\-\\s]", "").toUpperCase(Locale.ROOT);
        if ("RUT".equals(tipo)) {
            if (!normalizado.matches("[0-9]{7,8}[0-9K]") || Reglas.digitoVerificador(
                    normalizado.substring(0, normalizado.length() - 1)) != normalizado.charAt(normalizado.length() - 1)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dni: Ingresa un RUT válido.");
            }
        } else if ("PASAPORTE".equals(tipo)) {
            if (!normalizado.matches("[A-Z0-9]{5,20}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dni: Debe tener entre 5 y 20 letras o dígitos.");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipoDocumento: Selecciona RUT o PASAPORTE.");
        }
        return normalizado;
    }
}
