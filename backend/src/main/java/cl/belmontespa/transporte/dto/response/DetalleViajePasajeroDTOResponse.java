package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.DetalleViajePasajero;

public record DetalleViajePasajeroDTOResponse(
		String folio,
		String dniHash,
		String nombrePasajero
) {

	public static DetalleViajePasajeroDTOResponse from(DetalleViajePasajero detalle) {
		return new DetalleViajePasajeroDTOResponse(
				detalle.getViaje().getFolio(),
				detalle.getPasajero().getDniHash(),
				detalle.getPasajero().getNombre());
	}
}
