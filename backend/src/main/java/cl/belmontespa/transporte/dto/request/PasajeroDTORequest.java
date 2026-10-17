package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// dni en texto plano (RUT o pasaporte); PasajeroService lo hashea en dni_hash.
public record PasajeroDTORequest(
		@NotBlank @Size(max = 20) String dni,
		@NotBlank @Size(max = 120) String nombre
) {}
