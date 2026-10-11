package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.EstadoConductor;

public interface EstadoConductorRepository extends JpaRepository<EstadoConductor, Long> {
}
