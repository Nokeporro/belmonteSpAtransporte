package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.DetalleViajePasajero;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.model.Pago;
import cl.belmontespa.transporte.model.Solicitante;
import cl.belmontespa.transporte.model.Viaje;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// conductor, móvil y pago son NULL hasta la asignación / el registro del pago.
public record ViajeDTOResponse(
		String folio,
		String estado,
		String correoSolicitante,
		String nombreSolicitante,
		String telefonoSolicitante,
		String razonSocial,
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
		String motivo,
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

	public static ViajeDTOResponse from(Viaje viaje, List<DetalleViajePasajero> pasajeros) {
		Solicitante solicitante = viaje.getSolicitante();
		Conductor conductor = viaje.getConductor();
		Movil movil = viaje.getMovil();
		Pago pago = viaje.getPago();
		return new ViajeDTOResponse(
				viaje.getFolio(),
				viaje.getEstado().getNombreEstado(),
				solicitante.getUsuario().getCorreo(),
				solicitante.getUsuario().getNombre(),
				solicitante.getUsuario().getTelefono(),
				solicitante.getEmpresa().getRazonSocial(),
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
				viaje.getMotivo(),
				pasajeros.stream().map(PasajeroDTOResponse::from).toList());
	}

	// Al Conductor no le llega nada del pago (README, "Recursos y roles").
	public ViajeDTOResponse sinPago() {
		return new ViajeDTOResponse(folio, estado, correoSolicitante, nombreSolicitante, telefonoSolicitante,
				razonSocial, origen, destino, fhCreacion, fhAgenda, fhAsignacion, fhInicio, fhFin, numeroVuelo,
				cantidadPasajeros, correoConductor, nombreConductor, patenteMovil, null, null, motivo, pasajeros);
	}
}
