package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarViajeDTORequest(
		@NotBlank @Size(min = 5, max = 200) String motivo,
		@Min(0) Integer montoReembolso
) {

	public CancelarViajeDTORequest {
		motivo = Normalizar.texto(motivo);
	}
}
