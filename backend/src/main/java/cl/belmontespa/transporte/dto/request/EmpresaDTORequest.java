package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaDTORequest(
		@NotBlank @Size(max = 50) String razonSocial,
		@NotBlank @Size(max = 15) @Pattern(regexp = "^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-[\\dkK]$") String rut
) {}
