package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Solicitante;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {

	Optional<Solicitante> findByUsuarioCorreoIgnoreCase(String correo);

	@EntityGraph(attributePaths = {"usuario", "empresa", "estadoSolicitante"})
	List<Solicitante> findByEstadoSolicitanteEstado(String estado, Sort sort);
}
