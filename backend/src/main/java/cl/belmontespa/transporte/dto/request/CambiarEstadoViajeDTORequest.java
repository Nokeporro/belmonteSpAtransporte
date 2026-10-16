package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoViajeDTORequest(
		@NotBlank @Pattern(regexp = "EN_CURSO|FINALIZADO", flags = Pattern.Flag.CASE_INSENSITIVE) String estado
) {}
