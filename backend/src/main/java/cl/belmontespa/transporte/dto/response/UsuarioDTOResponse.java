package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Usuario;
import java.time.LocalDateTime;

public record UsuarioDTOResponse(
		String nombre,
		String correo,
		String telefono,
		LocalDateTime fechaRegistro
) {

	public static UsuarioDTOResponse from(Usuario usuario) {
		return new UsuarioDTOResponse(
				usuario.getNombre(),
				usuario.getCorreo(),
				usuario.getTelefono(),
				usuario.getFechaRegistro());
	}
}
