package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoPagoDTORequest(
		@NotBlank @Pattern(regexp = "PAGADO|RECHAZADO|ANULADO", flags = Pattern.Flag.CASE_INSENSITIVE) String estado
) {}
