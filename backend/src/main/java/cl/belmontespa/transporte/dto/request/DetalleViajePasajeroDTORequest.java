package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DetalleViajePasajeroDTORequest(
		@NotBlank @Size(max = 20) String folio,
		@NotBlank @Size(max = 50) String dniHash
) {}
