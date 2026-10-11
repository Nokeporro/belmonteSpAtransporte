package repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import model.DetalleViajePasajero;
import model.DetalleViajePasajeroId;

public interface DetalleViajePasajeroRepository extends JpaRepository<DetalleViajePasajero, DetalleViajePasajeroId> {
    List<DetalleViajePasajero> findByViaje_IdViaje(Long idViaje);
}
