package cl.belmontespa.transporte.dto.response;

import java.time.LocalDateTime;

public record PasswordTemporalDTOResponse(
		String correo,
		String passwordTemporal,
		LocalDateTime expira
) {}
