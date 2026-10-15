package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Movil;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovilRepository extends JpaRepository<Movil, Long> {

	Optional<Movil> findByPatente(String patente);
}
