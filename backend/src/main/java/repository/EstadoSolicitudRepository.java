package repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.EstadoSolicitud;

public interface EstadoSolicitudRepository extends JpaRepository<EstadoSolicitud, Long> {

    /**
     * Para resolver el id del estado por su nombre al cambiar el estado de un Viaje
     * (ej. findByNombreEstado("EN_CURSO")), sin hardcodear ids.
     */
    Optional<EstadoSolicitud> findByNombreEstado(String nombreEstado);
}
