package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.Usuario;
import java.time.LocalDateTime;

public record ConductorDTOResponse(
		String nombre,
		String correo,
		String telefono,
		LocalDateTime fechaRegistro,
		String estadoConductor
) {

	public static ConductorDTOResponse from(Conductor conductor) {
		Usuario usuario = conductor.getUsuario();
		return new ConductorDTOResponse(
				usuario.getNombre(),
				usuario.getCorreo(),
				usuario.getTelefono(),
				usuario.getFechaRegistro(),
				conductor.getEstadoConductor().getEstado());
	}
}
