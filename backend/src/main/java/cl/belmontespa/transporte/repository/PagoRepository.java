package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Pago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

	Optional<Pago> findByHash(String hash);
}
