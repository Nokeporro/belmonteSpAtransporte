package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// dni en texto plano (RUT o pasaporte); PasajeroService lo hashea en dni_hash.
public record PasajeroDTORequest(
		@NotBlank @jakarta.validation.constraints.Pattern(regexp = "RUT|PASAPORTE") String tipoDocumento,
		@NotBlank @Size(max = 20, message = Reglas.MAXIMO) String dni,
		@NotBlank @Size(min = 2, max = 120) String nombre
) {
	public PasajeroDTORequest(String dni, String nombre) { this("RUT", dni, nombre); }

	public PasajeroDTORequest {
		nombre = Normalizar.texto(nombre);
		dni = dni == null ? null : dni.replaceAll("[.\\-\\s]", "").toUpperCase(java.util.Locale.ROOT);
	}
}
