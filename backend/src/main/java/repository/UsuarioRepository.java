package repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Usado en el login (RF01/RF06): busca la cuenta por correo para validar la
     * contraseña y emitir el JWT.
     */
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
