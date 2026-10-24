package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Normalizar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// V8 (CU-16/RF24): el conductor asignado devuelve el viaje a PENDIENTE y dice por qué.
public record RechazarViajeDTORequest(
		@NotBlank @Size(min = 5, max = 200) String motivo
) {

	public RechazarViajeDTORequest {
		motivo = Normalizar.texto(motivo);
	}
}
