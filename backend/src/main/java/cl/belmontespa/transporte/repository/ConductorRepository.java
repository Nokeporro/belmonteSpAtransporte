package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Conductor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

	Optional<Conductor> findByUsuarioCorreoIgnoreCase(String correo);
}
