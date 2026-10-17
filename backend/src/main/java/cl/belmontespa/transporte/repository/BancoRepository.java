package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Banco;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BancoRepository extends JpaRepository<Banco, Long> {

	Optional<Banco> findByCodSbif(Integer codSbif);
}
