package com.belmontespa.transporte.controller;

import com.belmontespa.transporte.dto.request.LoginDTORequest;
import com.belmontespa.transporte.dto.response.LoginDTOResponse;
import com.belmontespa.transporte.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginDTOResponse login(@Valid @RequestBody LoginDTORequest request) {
        return authService.login(request.correo(), request.password());
    }
}
