package cl.belmontespa.transporte.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Solicitante (JWT), folio y estado (PENDIENTE) los fija ViajeService (CU-01/RF02).
public record ViajeDTORequest(
		@NotNull @Future LocalDateTime fhAgenda,
		@Valid @NotNull Ubicacion origen,
		@Valid @NotNull Ubicacion destino,
		@Size(max = 20) String numeroVuelo
) {

	// La comuna se identifica por nombre + región: su UNIQUE es (nombre, id_region).
	public record Ubicacion(
			@NotBlank @Size(max = 200) String direccion,
			@NotBlank @Size(max = 80) String comuna,
			@NotBlank @Size(max = 60) String region,
			@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 3, fraction = 6) BigDecimal latitud,
			@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 6) BigDecimal longitud
	) {}
}
