package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// razonSocial y rutEmpresa crean o reutilizan la fila de EMPRESA (RF01).
public record SolicitanteDTORequest(
		@NotBlank @Size(max = 120) String nombre,
		@NotBlank @Email @Size(max = 150) String correo,
		@Size(max = 20) String telefono,
		@Size(min = 8, max = 100) String password,
		@NotBlank @Size(max = 50) String razonSocial,
		@NotBlank @Size(max = 15) @Pattern(regexp = "^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-[\\dkK]$") String rutEmpresa
) {}
