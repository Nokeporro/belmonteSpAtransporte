package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.LoginDTORequest;
import cl.belmontespa.transporte.dto.request.RecuperarPasswordDTORequest;
import cl.belmontespa.transporte.dto.response.LoginDTOResponse;
import cl.belmontespa.transporte.dto.response.PasswordTemporalDTOResponse;
import cl.belmontespa.transporte.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
		return authService.login(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(org.springframework.security.core.Authentication auth) {
		authService.logout(auth.getName());
	}

	@PostMapping("/recuperar-password")
	public PasswordTemporalDTOResponse recuperarPassword(@Valid @RequestBody RecuperarPasswordDTORequest request) {
		return authService.recuperarPassword(request);
	}
}
