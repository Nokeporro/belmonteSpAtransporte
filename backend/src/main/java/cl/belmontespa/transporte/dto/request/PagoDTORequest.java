package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// hash, estado (Pendiente), empresa y fecha_pago los fija PagoService (CU-11).
public record PagoDTORequest(
		@NotBlank @Size(max = 20) String folio,
		@NotNull @Positive Integer monto,
		@NotNull Integer codSbif,
		@NotBlank @Size(max = 20) String medioPago,
		@NotBlank @Size(max = 20) String tipoRecibo
) {}
