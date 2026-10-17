package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Comuna;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {

	// UNIQUE (nombre, id_region): el nombre solo no identifica la comuna.
	Optional<Comuna> findByNombreAndRegionNombre(String nombre, String nombreRegion);
}
