package com.belmontespa.transporte.repository;
import com.belmontespa.transporte.model.Solicitante;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {
    boolean existsByIdUsuario(Long idUsuario);
}
