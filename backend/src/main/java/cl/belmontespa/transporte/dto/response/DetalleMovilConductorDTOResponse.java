package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.DetalleMovilConductor;
import cl.belmontespa.transporte.model.Usuario;

public record DetalleMovilConductorDTOResponse(
		String patente,
		String correoConductor,
		String nombreConductor
) {

	public static DetalleMovilConductorDTOResponse from(DetalleMovilConductor detalle) {
		Usuario usuario = detalle.getConductor().getUsuario();
		return new DetalleMovilConductorDTOResponse(
				detalle.getMovil().getPatente(),
				usuario.getCorreo(),
				usuario.getNombre());
	}
}
