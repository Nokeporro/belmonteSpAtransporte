package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdministradorDTORequest(
		@NotBlank @Size(max = 120) String nombre,
		@NotBlank @Email @Size(max = 150) String correo,
		@Size(max = 20) String telefono,
		@Size(min = 8, max = 100) String password
) {}
