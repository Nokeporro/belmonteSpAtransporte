package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.LoginDTORequest;
import cl.belmontespa.transporte.dto.request.RecuperarPasswordDTORequest;
import cl.belmontespa.transporte.dto.response.LoginDTOResponse;
import cl.belmontespa.transporte.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Único punto de entrada sin JWT (README, "Autenticación").
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	// CU-14.
	@PostMapping("/login")
	public LoginDTOResponse login(@Valid @RequestBody LoginDTORequest request) {
		return authService.login(request);
	}

	// CU-13/RF21: siempre 204, exista o no el correo.
	@PostMapping("/recuperar-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void recuperarPassword(@Valid @RequestBody RecuperarPasswordDTORequest request) {
		authService.recuperarPassword(request);
	}
}
