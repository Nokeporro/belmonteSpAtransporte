package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.EstadoConductor;

public interface EstadoConductorRepository extends JpaRepository<EstadoConductor, Long> {
}
