package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCorreoIgnoreCase(String correo);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where lower(u.correo) = lower(:correo)")
	Optional<Usuario> bloquearPorCorreo(@Param("correo") String correo);

	boolean existsByCorreoIgnoreCase(String correo);

	@Modifying
	@Query("update Usuario u set u.tokenFcm = null where u.tokenFcm = :token and u.idUsuario <> :idUsuario")
	int quitarTokenFcm(@Param("token") String token, @Param("idUsuario") Long idUsuario);
}
