package com.belmontespa.transporte.dto.response;

public record LoginDTOResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String correo,
        String rol
) {}
