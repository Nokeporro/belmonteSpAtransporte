package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PagoDTORequest(
		@NotBlank @Pattern(regexp = Reglas.FOLIO) String folio,
		@NotNull @Positive Integer monto,
		@NotNull Integer codSbif,
		@NotBlank @Size(max = 20, message = Reglas.MAXIMO) String medioPago,
		@NotBlank @Size(max = 20, message = Reglas.MAXIMO) String tipoRecibo
) {}
