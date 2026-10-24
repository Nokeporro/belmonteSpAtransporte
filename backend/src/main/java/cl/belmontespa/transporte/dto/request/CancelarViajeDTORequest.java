package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// V7 (CU-15/RF23): solo lo envía el Administrador; el Solicitante cancela sin cuerpo (V5). El
// `montoReembolso` es obligatorio solo si el viaje está vencido y su pago PAGADO, y su tope (el monto
// del pago) lo revisa el servicio; si no se necesita, se ignora.
public record CancelarViajeDTORequest(
		@NotBlank @Size(min = 5, max = 200) String motivo,
		@Min(0) Integer montoReembolso
) {

	public CancelarViajeDTORequest {
		motivo = Normalizar.texto(motivo);
	}
}
