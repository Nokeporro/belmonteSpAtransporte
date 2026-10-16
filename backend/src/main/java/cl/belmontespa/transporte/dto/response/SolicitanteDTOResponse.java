package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Empresa;
import cl.belmontespa.transporte.model.Solicitante;
import cl.belmontespa.transporte.model.Usuario;
import java.time.LocalDateTime;

public record SolicitanteDTOResponse(
		String nombre,
		String correo,
		String telefono,
		LocalDateTime fechaRegistro,
		String razonSocial,
		String rutEmpresa
) {

	public static SolicitanteDTOResponse from(Solicitante solicitante) {
		Usuario usuario = solicitante.getUsuario();
		Empresa empresa = solicitante.getEmpresa();
		return new SolicitanteDTOResponse(
				usuario.getNombre(),
				usuario.getCorreo(),
				usuario.getTelefono(),
				usuario.getFechaRegistro(),
				empresa.getRazonSocial(),
				empresa.getRut());
	}
}
