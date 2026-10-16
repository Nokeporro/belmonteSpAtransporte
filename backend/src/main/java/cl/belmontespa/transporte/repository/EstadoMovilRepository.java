package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.EstadoMovil;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoMovilRepository extends JpaRepository<EstadoMovil, Long> {

	Optional<EstadoMovil> findByNombre(String nombre);
}
