package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.Solicitante;

public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {
}
