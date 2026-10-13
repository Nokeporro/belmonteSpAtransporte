package com.belmontespa.transporte.dto.response;

import java.time.LocalDateTime;

public record UsuarioDTOResponse(
        String nombre,
        String correo,
        String telefono,
        LocalDateTime fechaRegistro,
        String tokenFcm
) {}
