package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// Sin estado: el móvil nace disponible (CU-09).
public record MovilDTORequest(
		@NotBlank @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{2}[0-9]{2}$") String patente,
		@NotNull @Positive Integer capacidad,
		Integer gpsSerial
) {}
