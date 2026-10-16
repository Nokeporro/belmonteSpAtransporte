package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Anulado es automático al cancelar el viaje (CU-08); no pasa por aquí.
public record CambiarEstadoPagoDTORequest(
		@NotBlank @Pattern(regexp = "PAGADO|RECHAZADO", flags = Pattern.Flag.CASE_INSENSITIVE) String estado
) {}
