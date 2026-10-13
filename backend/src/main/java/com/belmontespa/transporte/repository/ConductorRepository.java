package com.belmontespa.transporte.repository;
import com.belmontespa.transporte.model.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    boolean existsByIdUsuario(Long idUsuario);
}
