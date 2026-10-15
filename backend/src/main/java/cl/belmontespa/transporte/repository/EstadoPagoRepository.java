package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.EstadoPago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoPagoRepository extends JpaRepository<EstadoPago, Long> {

	Optional<EstadoPago> findByEstado(String estado);
}
