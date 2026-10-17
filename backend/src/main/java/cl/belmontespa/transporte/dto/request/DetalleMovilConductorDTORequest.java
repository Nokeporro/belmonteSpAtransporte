package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DetalleMovilConductorDTORequest(
		@NotBlank @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{2}[0-9]{2}$") String patente,
		@NotBlank @Email @Size(max = 150) String correoConductor
) {}
