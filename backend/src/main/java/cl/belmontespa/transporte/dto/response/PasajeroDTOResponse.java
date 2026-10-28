package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.DetalleViajePasajero;
import cl.belmontespa.transporte.model.Pasajero;
import java.time.LocalDateTime;

public record PasajeroDTOResponse(
		String dniHash,
		String nombre,
		LocalDateTime fhAbordaje
) {

	public static PasajeroDTOResponse from(Pasajero pasajero) {
		return new PasajeroDTOResponse(pasajero.getDniHash(), pasajero.getNombre(), null);
	}

	public static PasajeroDTOResponse from(DetalleViajePasajero detalle) {
		Pasajero pasajero = detalle.getPasajero();
		return new PasajeroDTOResponse(pasajero.getDniHash(), pasajero.getNombre(), detalle.getFhAbordaje());
	}
}
