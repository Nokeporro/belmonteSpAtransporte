package repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Ubicacion;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    /**
     * Reutiliza una UBICACION existente si la dirección (de Google Places) ya fue
     * registrada antes, en vez de crear una fila duplicada.
     */
    Optional<Ubicacion> findByDireccion(String direccion);
}
