package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ViajeDTORequest(
		@NotNull @Future LocalDateTime fhAgenda,
		@Valid @NotNull Ubicacion origen,
		@Valid @NotNull Ubicacion destino,
		@Pattern(regexp = Reglas.NUMERO_VUELO) String numeroVuelo
) {

	public ViajeDTORequest {
		numeroVuelo = Normalizar.numeroVuelo(numeroVuelo);
	}

	public record Ubicacion(
			@NotBlank @Size(max = 200, message = Reglas.MAXIMO) String direccion,
			@NotBlank @Size(max = 80, message = Reglas.MAXIMO) String comuna,
			@NotBlank @Size(max = 60, message = Reglas.MAXIMO) String region,
			@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 3, fraction = 6) BigDecimal latitud,
			@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 6) BigDecimal longitud
	) {

		public Ubicacion {
			direccion = Normalizar.texto(direccion);
			comuna = Normalizar.texto(comuna);
			region = Normalizar.texto(region);
		}
	}
}
