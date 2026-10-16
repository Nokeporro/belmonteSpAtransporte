package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.EstadoConductor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoConductorRepository extends JpaRepository<EstadoConductor, Long> {

	Optional<EstadoConductor> findByEstado(String estado);
}
