package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.config.JwtProvider;
import cl.belmontespa.transporte.dto.request.LoginDTORequest;
import cl.belmontespa.transporte.dto.request.RecuperarPasswordDTORequest;
import cl.belmontespa.transporte.dto.response.LoginDTOResponse;
import cl.belmontespa.transporte.dto.response.PasswordTemporalDTOResponse;
import cl.belmontespa.transporte.model.Usuario;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import cl.belmontespa.transporte.repository.ConductorRepository;
import cl.belmontespa.transporte.repository.SolicitanteRepository;
import cl.belmontespa.transporte.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Clock;
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
	private final Clock clock;
	private final long horasPasswordTemporal;
	private final SecureRandom random = new SecureRandom();

	public AuthService(UsuarioRepository usuarioRepository, AdministradorRepository administradorRepository,
			ConductorRepository conductorRepository, SolicitanteRepository solicitanteRepository,
			PasswordEncoder passwordEncoder, JwtProvider jwtProvider, Clock clock,
			@Value("${app.password-temporal.horas:24}") long horasPasswordTemporal) {
		this.usuarioRepository = usuarioRepository;
		this.administradorRepository = administradorRepository;
		this.conductorRepository = conductorRepository;
		this.solicitanteRepository = solicitanteRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtProvider = jwtProvider;
		this.clock = clock;
		this.horasPasswordTemporal = horasPasswordTemporal;
	}

	@Transactional
	public LoginDTOResponse login(LoginDTORequest request) {
		Usuario usuario = usuarioRepository.bloquearPorCorreo(request.correo())
				.filter(u -> contrasenaValida(request.password(), u))
				.orElseThrow(AuthService::credencialesInvalidas);
		String rol = rol(usuario.getIdUsuario());
		boolean temporal = !passwordEncoder.matches(request.password(), usuario.getPasswordHash());
		if ("CONDUCTOR".equals(rol) && (temporal || !conductorRepository.findById(usuario.getIdUsuario()).orElseThrow().isActivo())) {
			throw credencialesInvalidas();
		}
		if ("SOLICITANTE".equals(rol)) {
			String estado = solicitanteRepository.findById(usuario.getIdUsuario()).orElseThrow().getEstadoSolicitante().getEstado();
			if (!"APROBADO".equals(estado)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PENDIENTE".equals(estado)
						? "Tu cuenta espera la aprobación de un Administrador."
						: "Tu cuenta no fue aprobada. Contacta a Belmonte SpA.");
			}
		}
		if (!temporal) {
			usuario.setPasswordTemporalHash(null);
			usuario.setPasswordTemporalExpira(null);
		}
		if (request.tokenFcm() != null && !request.tokenFcm().isBlank()) {
			usuarioRepository.quitarTokenFcm(request.tokenFcm(), usuario.getIdUsuario());
			usuario.setTokenFcm(request.tokenFcm());
		}
		return new LoginDTOResponse(jwtProvider.generar(usuario.getCorreo(), rol, temporal), "Bearer",
				jwtProvider.getExpirationSeconds(), usuario.getCorreo(), rol, temporal);
	}

	@Transactional
	public void logout(String correo) {
		usuarioRepository.findByCorreoIgnoreCase(correo).ifPresent(u -> u.setTokenFcm(null));
	}

	@Transactional
	public PasswordTemporalDTOResponse recuperarPassword(RecuperarPasswordDTORequest request) {
		Usuario usuario = usuarioRepository.bloquearPorCorreo(request.correo())
				.filter(u -> solicitanteRepository.existsById(u.getIdUsuario())
						|| administradorRepository.existsById(u.getIdUsuario()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		String passwordTemporal = passwordTemporal();
		usuario.setPasswordTemporalHash(passwordEncoder.encode(passwordTemporal));
		usuario.setPasswordTemporalExpira(LocalDateTime.now(clock).plusHours(horasPasswordTemporal).withNano(0));
		return new PasswordTemporalDTOResponse(usuario.getCorreo(), passwordTemporal, usuario.getPasswordTemporalExpira());
	}

	private boolean contrasenaValida(String password, Usuario usuario) {
		if (passwordEncoder.matches(password, usuario.getPasswordHash())) {
			return true;
		}
		return usuario.getPasswordTemporalHash() != null
				&& usuario.getPasswordTemporalExpira() != null
				&& usuario.getPasswordTemporalExpira().isAfter(LocalDateTime.now(clock))
				&& passwordEncoder.matches(password, usuario.getPasswordTemporalHash());
	}

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
