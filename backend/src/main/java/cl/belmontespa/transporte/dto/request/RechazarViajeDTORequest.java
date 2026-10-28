package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarViajeDTORequest(
		@NotBlank @Size(min = 5, max = 200) String motivo
) {

	public RechazarViajeDTORequest {
		motivo = Normalizar.texto(motivo);
	}
}
