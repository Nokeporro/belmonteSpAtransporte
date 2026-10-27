package cl.belmontespa.transporte.dto.response;

import java.time.LocalDateTime;

// La contraseña temporal viaja en claro solo en esta respuesta (README, "Autenticación").
public record PasswordTemporalDTOResponse(
		String correo,
		String passwordTemporal,
		LocalDateTime expira
) {}
