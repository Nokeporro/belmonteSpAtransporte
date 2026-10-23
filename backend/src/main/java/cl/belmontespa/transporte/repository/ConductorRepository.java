package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Conductor;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

	@EntityGraph(attributePaths = {"usuario", "estadoConductor"})
	Optional<Conductor> findByUsuarioCorreoIgnoreCase(String correo);

	@Override
	@EntityGraph(attributePaths = {"usuario", "estadoConductor"})
	List<Conductor> findAll(Sort sort);
}
