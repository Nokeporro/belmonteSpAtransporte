package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AbordajeDTORequest(
		@NotBlank @Pattern(regexp = Reglas.PATENTE) String patente,
		@NotBlank @Pattern(regexp = Reglas.CODIGO_ABORDAJE) String codigoAbordaje,
		@NotBlank @Pattern(regexp = "RUT|PASAPORTE") String tipoDocumento,
		@NotBlank @Size(max = 20, message = Reglas.MAXIMO) String dni
) {

	public AbordajeDTORequest {
		patente = Normalizar.patente(patente);
		codigoAbordaje = Normalizar.codigoAbordaje(codigoAbordaje);
		dni = dni == null ? null : dni.replaceAll("[.\\-\\s]", "").toUpperCase(java.util.Locale.ROOT);
	}
}
