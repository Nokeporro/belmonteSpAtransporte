package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Pasajero;

public record PasajeroDTOResponse(
		String dniHash,
		String nombre
) {

	public static PasajeroDTOResponse from(Pasajero pasajero) {
		return new PasajeroDTOResponse(pasajero.getDniHash(), pasajero.getNombre());
	}
}
