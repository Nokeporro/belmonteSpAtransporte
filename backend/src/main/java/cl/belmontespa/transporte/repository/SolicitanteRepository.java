package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Solicitante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {

	Optional<Solicitante> findByUsuarioCorreoIgnoreCase(String correo);
}
