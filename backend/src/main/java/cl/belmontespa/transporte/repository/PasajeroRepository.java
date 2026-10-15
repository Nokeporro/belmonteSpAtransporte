package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Pasajero;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long> {

	Optional<Pasajero> findByDniHash(String dniHash);
}
