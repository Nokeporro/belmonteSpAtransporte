package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// Sin estado: el móvil nace disponible (CU-09).
public record MovilDTORequest(
		@NotBlank @Pattern(regexp = Reglas.PATENTE) String patente,
		@NotNull @Min(1) @Max(20) Integer capacidad,
		@Positive Integer gpsSerial
) {

	public MovilDTORequest {
		patente = Normalizar.patente(patente);
	}
}
