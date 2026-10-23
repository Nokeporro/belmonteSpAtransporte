package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.DetalleMovilConductor;
import cl.belmontespa.transporte.model.Usuario;

public record DetalleMovilConductorDTOResponse(
		String patente,
		String correoConductor,
		String nombreConductor,
		Integer capacidad,
		String estadoMovil,
		boolean activoMovil,
		String estadoConductor,
		boolean activoConductor
) {

	public static DetalleMovilConductorDTOResponse from(DetalleMovilConductor detalle) {
		Usuario usuario = detalle.getConductor().getUsuario();
		return new DetalleMovilConductorDTOResponse(
				detalle.getMovil().getPatente(),
				usuario.getCorreo(),
				usuario.getNombre(),
				detalle.getMovil().getCapacidad(),
				detalle.getMovil().getEstadoMovil().getNombre(),
				detalle.getMovil().isActivo(),
				detalle.getConductor().getEstadoConductor().getEstado(),
				detalle.getConductor().isActivo());
	}
}
