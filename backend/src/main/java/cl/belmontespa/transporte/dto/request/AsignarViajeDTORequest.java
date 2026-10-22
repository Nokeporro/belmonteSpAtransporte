package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AsignarViajeDTORequest(
		@NotBlank @Pattern(regexp = Reglas.CORREO) @Size(max = 150, message = Reglas.MAXIMO) String correoConductor,
		@NotBlank @Pattern(regexp = Reglas.PATENTE) String patente
) {

	public AsignarViajeDTORequest {
		correoConductor = Normalizar.correo(correoConductor);
		patente = Normalizar.patente(patente);
	}
}
