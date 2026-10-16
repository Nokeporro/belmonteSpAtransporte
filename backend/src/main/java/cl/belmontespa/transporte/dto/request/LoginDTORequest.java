package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// tokenFcm opcional: si viene, reemplaza USUARIO.token_fcm (RF13).
public record LoginDTORequest(
		@NotBlank @Email String correo,
		@NotBlank String password,
		@Size(max = 255) String tokenFcm
) {}
