package com.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDTORequest(
        @NotBlank @Email String correo,
        @NotBlank String password
) {}
