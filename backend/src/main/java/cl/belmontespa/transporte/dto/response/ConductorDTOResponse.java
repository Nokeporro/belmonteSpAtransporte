package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.Usuario;
import java.time.LocalDateTime;

public record ConductorDTOResponse(
		String nombre,
		String correo,
		String telefono,
		LocalDateTime fechaRegistro,
		boolean activo,
		String estadoConductor,
		long viajesAsignados,
		String folioEnCurso
) {

	public static ConductorDTOResponse from(Conductor conductor, long viajesAsignados, String folioEnCurso) {
		Usuario usuario = conductor.getUsuario();
		return new ConductorDTOResponse(
				usuario.getNombre(),
				usuario.getCorreo(),
				usuario.getTelefono(),
				usuario.getFechaRegistro(),
				conductor.isActivo(),
				conductor.getEstadoConductor().getEstado(),
				viajesAsignados,
				folioEnCurso);
	}
}
