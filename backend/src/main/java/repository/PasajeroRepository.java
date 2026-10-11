package repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Pasajero;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long> {
    Optional<Pasajero> findByRutHash(String rutHash);
}
