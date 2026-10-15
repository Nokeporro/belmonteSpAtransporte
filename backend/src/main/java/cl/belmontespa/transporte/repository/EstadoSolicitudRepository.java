package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.EstadoSolicitud;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoSolicitudRepository extends JpaRepository<EstadoSolicitud, Long> {

	Optional<EstadoSolicitud> findByNombreEstado(String nombreEstado);
}
