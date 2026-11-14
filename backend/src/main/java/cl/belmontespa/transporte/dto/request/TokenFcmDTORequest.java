package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenFcmDTORequest(
		@NotBlank @Size(max = 255, message = Reglas.MAXIMO) String tokenFcm
) {
}
