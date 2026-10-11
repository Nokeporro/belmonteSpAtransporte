package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Solicitante;

public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {
}
