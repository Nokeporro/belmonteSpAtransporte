package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.UsuarioDTORequest;
import cl.belmontespa.transporte.dto.response.UsuarioDTOResponse;
import cl.belmontespa.transporte.model.Usuario;
import cl.belmontespa.transporte.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService extends CrudService<Usuario, String, UsuarioDTORequest, UsuarioDTOResponse> {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final java.time.Clock clock;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, java.time.Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	protected JpaRepository<Usuario, Long> repositorio() {
		return usuarioRepository;
	}

	@Override
	protected Optional<Usuario> buscar(String correo) {
		return usuarioRepository.findByCorreoIgnoreCase(correo);
	}

	@Override
	@Transactional
	public UsuarioDTOResponse actualizar(String correo, UsuarioDTORequest request) {
		Usuario usuario = usuarioRepository.bloquearPorCorreo(correo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		reemplazar(usuario, request);
		return aResponse(usuario);
	}

	@Override
	protected void reemplazar(Usuario usuario, UsuarioDTORequest request) {
		if (!usuario.getCorreo().equalsIgnoreCase(request.correo())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "correo: Debe coincidir con el de la ruta.");
		}
		if (request.password() != null && !request.password().isBlank()) {
			String actual = request.passwordActual();
			boolean normal = actual != null && passwordEncoder.matches(actual, usuario.getPasswordHash());
			boolean temporal = actual != null && usuario.getPasswordTemporalHash() != null
					&& usuario.getPasswordTemporalExpira() != null
					&& usuario.getPasswordTemporalExpira().isAfter(java.time.LocalDateTime.now(clock))
					&& passwordEncoder.matches(actual, usuario.getPasswordTemporalHash());
			if (!normal && !temporal) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passwordActual: No coincide.");
		}
		reemplazarDatos(usuario, request.nombre(), usuario.getCorreo(), request.telefono(), request.password());
	}

	@Override
	protected UsuarioDTOResponse aResponse(Usuario usuario) {
		return UsuarioDTOResponse.from(usuario);
	}

	Usuario nuevo(String nombre, String correo, String telefono, String password) {
		Usuario usuario = prepararAlta(nombre, correo, telefono, password);
		usuario.setPasswordHash(passwordEncoder.encode(password));
		return usuarioRepository.saveAndFlush(usuario);
	}

	Usuario nuevoConTemporal(String nombre, String correo, String telefono, String password, long horas) {
		Usuario usuario = prepararAlta(nombre, correo, telefono, password);
		usuario.setPasswordHash("*");
		usuario.setPasswordTemporalHash(passwordEncoder.encode(password));
		usuario.setPasswordTemporalExpira(java.time.LocalDateTime.now(clock).plusHours(horas).withNano(0));
		return usuarioRepository.saveAndFlush(usuario);
	}

	private Usuario prepararAlta(String nombre, String correo, String telefono, String password) {
		if (password == null || password.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
		}
		if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
		}
		Usuario usuario = new Usuario();
		usuario.setNombre(nombre);
		usuario.setCorreo(correo);
		usuario.setTelefono(telefono);
		return usuario;
	}

	void reemplazarDatos(Usuario usuario, String nombre, String correo, String telefono, String password) {
		if (!usuario.getCorreo().equalsIgnoreCase(correo) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
		}
		usuario.setNombre(nombre);
		usuario.setCorreo(correo);
		usuario.setTelefono(telefono);
		if (password != null && !password.isBlank()) {
			usuario.setPasswordHash(passwordEncoder.encode(password));
			usuario.setPasswordTemporalHash(null);
			usuario.setPasswordTemporalExpira(null);
		}
	}
}
