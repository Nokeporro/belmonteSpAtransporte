package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.FechaHora;

public interface FechaHoraRepository extends JpaRepository<FechaHora, Long> {
}
