package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.client.EmailClient;
import cl.belmontespa.transporte.config.JwtProvider;
import cl.belmontespa.transporte.dto.request.LoginDTORequest;
import cl.belmontespa.transporte.dto.request.RecuperarPasswordDTORequest;
import cl.belmontespa.transporte.dto.response.LoginDTOResponse;
import cl.belmontespa.transporte.model.Usuario;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import cl.belmontespa.transporte.repository.ConductorRepository;
import cl.belmontespa.transporte.repository.SolicitanteRepository;
import cl.belmontespa.transporte.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
	private static final int LARGO_PASSWORD_TEMPORAL = 12;

	private final UsuarioRepository usuarioRepository;
	private final AdministradorRepository administradorRepository;
	private final ConductorRepository conductorRepository;
	private final SolicitanteRepository solicitanteRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final EmailClient emailClient;
	private final long minutosPasswordTemporal;
	private final SecureRandom random = new SecureRandom();

	public AuthService(UsuarioRepository usuarioRepository, AdministradorRepository administradorRepository,
			ConductorRepository conductorRepository, SolicitanteRepository solicitanteRepository,
			PasswordEncoder passwordEncoder, JwtProvider jwtProvider, EmailClient emailClient,
			@Value("${app.password-temporal.minutos:30}") long minutosPasswordTemporal) {
		this.usuarioRepository = usuarioRepository;
		this.administradorRepository = administradorRepository;
		this.conductorRepository = conductorRepository;
		this.solicitanteRepository = solicitanteRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtProvider = jwtProvider;
		this.emailClient = emailClient;
		this.minutosPasswordTemporal = minutosPasswordTemporal;
	}

	// CU-14: cualquier fallo da el mismo 401, sin indicar qué falló.
	@Transactional
	public LoginDTOResponse login(LoginDTORequest request) {
		Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo())
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.filter(u -> u.getPasswordTemporalExpira() == null
						|| u.getPasswordTemporalExpira().isAfter(LocalDateTime.now()))
				.orElseThrow(AuthService::credencialesInvalidas);
		String rol = rol(usuario.getIdUsuario());
		if (request.tokenFcm() != null && !request.tokenFcm().isBlank()) {
			usuario.setTokenFcm(request.tokenFcm());
		}
		return new LoginDTOResponse(jwtProvider.generar(usuario.getCorreo(), rol), "Bearer",
				jwtProvider.getExpirationSeconds(), usuario.getCorreo(), rol);
	}

	// CU-13/RF21: solo Solicitante y Administrador; nunca revela si el correo existe.
	@Transactional
	public void recuperarPassword(RecuperarPasswordDTORequest request) {
		usuarioRepository.findByCorreoIgnoreCase(request.correo())
				.filter(u -> solicitanteRepository.existsById(u.getIdUsuario())
						|| administradorRepository.existsById(u.getIdUsuario()))
				.ifPresent(usuario -> {
					String passwordTemporal = passwordTemporal();
					usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
					usuario.setPasswordTemporalExpira(LocalDateTime.now().plusMinutes(minutosPasswordTemporal));
					emailClient.enviarPasswordTemporal(usuario.getCorreo(), passwordTemporal, minutosPasswordTemporal);
				});
	}

	// Rol único por USUARIO_ROL_ACTIVO.
	private String rol(Long idUsuario) {
		if (administradorRepository.existsById(idUsuario)) {
			return "ADMINISTRADOR";
		}
		if (conductorRepository.existsById(idUsuario)) {
			return "CONDUCTOR";
		}
		if (solicitanteRepository.existsById(idUsuario)) {
			return "SOLICITANTE";
		}
		throw credencialesInvalidas();
	}

	private String passwordTemporal() {
		StringBuilder password = new StringBuilder(LARGO_PASSWORD_TEMPORAL);
		for (int i = 0; i < LARGO_PASSWORD_TEMPORAL; i++) {
			password.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
		}
		return password.toString();
	}

	private static ResponseStatusException credencialesInvalidas() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
	}
}
