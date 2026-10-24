package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Pago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

	Optional<Pago> findByHash(String hash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pago p where p.hash = :hash")
	Optional<Pago> bloquearPorHash(@Param("hash") String hash);
}
