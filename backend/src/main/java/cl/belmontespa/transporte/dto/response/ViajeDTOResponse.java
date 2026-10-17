package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.model.Pago;
import cl.belmontespa.transporte.model.Pasajero;
import cl.belmontespa.transporte.model.Viaje;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// conductor, móvil y pago son NULL hasta la asignación / el registro del pago.
public record ViajeDTOResponse(
		String folio,
		String estado,
		String correoSolicitante,
		Ubicacion origen,
		Ubicacion destino,
		LocalDateTime fhCreacion,
		LocalDateTime fhAgenda,
		LocalDateTime fhAsignacion,
		LocalDateTime fhInicio,
		LocalDateTime fhFin,
		String numeroVuelo,
		Integer cantidadPasajeros,
		String correoConductor,
		String nombreConductor,
		String patenteMovil,
		String hashPago,
		String estadoPago,
		List<PasajeroDTOResponse> pasajeros
) {

	public record Ubicacion(
			String direccion,
			String comuna,
			String region,
			BigDecimal latitud,
			BigDecimal longitud
	) {

		static Ubicacion from(cl.belmontespa.transporte.model.Ubicacion ubicacion) {
			return new Ubicacion(
					ubicacion.getDireccion(),
					ubicacion.getComuna().getNombre(),
					ubicacion.getComuna().getRegion().getNombre(),
					ubicacion.getLatitud(),
					ubicacion.getLongitud());
		}
	}

	public static ViajeDTOResponse from(Viaje viaje, List<Pasajero> pasajeros) {
		Conductor conductor = viaje.getConductor();
		Movil movil = viaje.getMovil();
		Pago pago = viaje.getPago();
		return new ViajeDTOResponse(
				viaje.getFolio(),
				viaje.getEstado().getNombreEstado(),
				viaje.getSolicitante().getUsuario().getCorreo(),
				Ubicacion.from(viaje.getOrigen()),
				Ubicacion.from(viaje.getDestino()),
				viaje.getFhCreacion(),
				viaje.getFhAgenda(),
				viaje.getFhAsignacion(),
				viaje.getFhInicio(),
				viaje.getFhFin(),
				viaje.getNumeroVuelo(),
				viaje.getCantidadPasajeros(),
				conductor == null ? null : conductor.getUsuario().getCorreo(),
				conductor == null ? null : conductor.getUsuario().getNombre(),
				movil == null ? null : movil.getPatente(),
				pago == null ? null : pago.getHash(),
				pago == null ? null : pago.getEstadoPago().getEstado(),
				pasajeros.stream().map(PasajeroDTOResponse::from).toList());
	}
}
