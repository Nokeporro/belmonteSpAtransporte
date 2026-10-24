package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Conductor;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Conductor c join c.usuario u where lower(u.correo) = lower(:correo)")
	Optional<Conductor> bloquearPorCorreo(String correo);

	@EntityGraph(attributePaths = {"usuario", "estadoConductor"})
	Optional<Conductor> findByUsuarioCorreoIgnoreCase(String correo);

	@Override
	@EntityGraph(attributePaths = {"usuario", "estadoConductor"})
	List<Conductor> findAll(Sort sort);
}
