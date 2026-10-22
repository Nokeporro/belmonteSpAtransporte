package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.EstadoSolicitante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoSolicitanteRepository extends JpaRepository<EstadoSolicitante, Long> {

	Optional<EstadoSolicitante> findByEstado(String estado);
}
