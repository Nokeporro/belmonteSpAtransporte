package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Administrador;
import cl.belmontespa.transporte.model.Usuario;
import java.time.LocalDateTime;

public record AdministradorDTOResponse(
		String nombre,
		String correo,
		String telefono,
		LocalDateTime fechaRegistro
) {

	public static AdministradorDTOResponse from(Administrador administrador) {
		Usuario usuario = administrador.getUsuario();
		return new AdministradorDTOResponse(
				usuario.getNombre(),
				usuario.getCorreo(),
				usuario.getTelefono(),
				usuario.getFechaRegistro());
	}
}
