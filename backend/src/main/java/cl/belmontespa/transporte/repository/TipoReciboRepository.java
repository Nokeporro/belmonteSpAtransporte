package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.TipoRecibo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoReciboRepository extends JpaRepository<TipoRecibo, Long> {

	Optional<TipoRecibo> findByNombre(String nombre);
}
