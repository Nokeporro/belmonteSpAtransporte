package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.MedioPago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedioPagoRepository extends JpaRepository<MedioPago, Long> {

	Optional<MedioPago> findByNombre(String nombre);
}
