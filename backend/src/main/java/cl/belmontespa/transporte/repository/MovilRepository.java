package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Movil;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface MovilRepository extends JpaRepository<Movil, Long> {

	Optional<Movil> findByPatente(String patente);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Movil m where m.patente = :patente")
	Optional<Movil> bloquearPorPatente(String patente);
}
