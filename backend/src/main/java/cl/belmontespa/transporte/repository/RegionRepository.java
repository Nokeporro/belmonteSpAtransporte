package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Region;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

	Optional<Region> findByNombre(String nombre);
}
