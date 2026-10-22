package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DetalleViajePasajeroDTORequest(
		@NotBlank @Pattern(regexp = Reglas.FOLIO) String folio,
		@NotBlank @Pattern(regexp = Reglas.DNI_HASH) String dniHash
) {}
