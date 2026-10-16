package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Administrador;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

	Optional<Administrador> findByUsuarioCorreoIgnoreCase(String correo);
}
