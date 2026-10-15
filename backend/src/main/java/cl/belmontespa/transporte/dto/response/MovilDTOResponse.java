package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Movil;

public record MovilDTOResponse(
		String patente,
		Integer capacidad,
		Integer gpsSerial,
		String estadoMovil
) {

	public static MovilDTOResponse from(Movil movil) {
		return new MovilDTOResponse(
				movil.getPatente(),
				movil.getCapacidad(),
				movil.getGpsSerial(),
				movil.getEstadoMovil().getNombre());
	}
}
