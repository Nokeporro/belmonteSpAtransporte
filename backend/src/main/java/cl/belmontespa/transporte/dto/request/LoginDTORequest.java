package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginDTORequest(
		@NotBlank @Pattern(regexp = Reglas.CORREO) @Size(max = 150, message = Reglas.MAXIMO) String correo,
		@NotBlank String password,
		@Size(max = 255, message = Reglas.MAXIMO) String tokenFcm
) {

	public LoginDTORequest {
		correo = Normalizar.correo(correo);
	}
}
