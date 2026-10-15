package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Usuario;
import com.belmontespa.transporte.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class UsuarioService extends CrudService<Usuario, Long> {
    private final PasswordEncoder passwordEncoder;
    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) { super(repository); this.passwordEncoder = passwordEncoder; }
    @Override public Usuario create(Usuario usuario) {
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        return super.create(usuario);
    }
    @Override public Usuario update(Long id, Usuario usuario) {
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) usuario.setPasswordHash(findById(id).getPasswordHash());
        else usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        return super.update(id, usuario);
    }
}
