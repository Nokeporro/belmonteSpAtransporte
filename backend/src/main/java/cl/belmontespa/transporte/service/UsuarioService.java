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
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService extends CrudService<Usuario, String, UsuarioDTORequest, UsuarioDTOResponse> {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
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
	protected Usuario nuevaEntidad(UsuarioDTORequest request) {
		return nuevo(request.nombre(), request.correo(), request.telefono(), request.password());
	}

	@Override
	protected void reemplazar(Usuario usuario, UsuarioDTORequest request) {
		reemplazarDatos(usuario, request.nombre(), request.correo(), request.telefono(), request.password());
	}

	@Override
	protected UsuarioDTOResponse aResponse(Usuario usuario) {
		return UsuarioDTOResponse.from(usuario);
	}

	// Usuario guardado y listo para extenderse con un rol (Administrador, Conductor, Solicitante).
	Usuario nuevo(String nombre, String correo, String telefono, String password) {
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
		usuario.setPasswordHash(passwordEncoder.encode(password));
		return usuarioRepository.saveAndFlush(usuario);
	}

	// PUT: password vacía conserva la actual; una nueva anula la contraseña temporal (RF21).
	void reemplazarDatos(Usuario usuario, String nombre, String correo, String telefono, String password) {
		if (!usuario.getCorreo().equalsIgnoreCase(correo) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
		}
		usuario.setNombre(nombre);
		usuario.setCorreo(correo);
		usuario.setTelefono(telefono);
		if (password != null && !password.isBlank()) {
			usuario.setPasswordHash(passwordEncoder.encode(password));
			usuario.setPasswordTemporalExpira(null);
		}
	}
}
