package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// P2/P3: el Administrador confirma, rechaza o anula; el reembolso es automático en V5/V7.
public record CambiarEstadoPagoDTORequest(
		@NotBlank @Pattern(regexp = "PAGADO|RECHAZADO|ANULADO", flags = Pattern.Flag.CASE_INSENSITIVE) String estado
) {}
