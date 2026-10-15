package com.belmontespa.transporte.service;

import com.belmontespa.transporte.config.JwtProvider;
import com.belmontespa.transporte.dto.response.LoginDTOResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    public LoginDTOResponse login(String correo, String password) {
        Authentication authentication = authenticationManager.authenticate(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                        .unauthenticated(correo, password));
        String token = jwtProvider.generateToken(authentication);
        String role = authentication.getAuthorities().stream()
                .findFirst().orElseThrow().getAuthority().replaceFirst("^ROLE_", "");
        return new LoginDTOResponse(token, "Bearer", jwtProvider.getExpirationSeconds(), authentication.getName(), role);
    }

}
