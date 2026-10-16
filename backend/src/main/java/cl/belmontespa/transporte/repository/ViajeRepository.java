package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Viaje;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

// Los finders cargan de una vez lo que lee ViajeDTOResponse.from() (open-in-view=false).
public interface ViajeRepository extends JpaRepository<Viaje, Long> {

	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "origen.comuna.region",
			"destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	Optional<Viaje> findByFolio(String folio);

	// RF03: historial del Solicitante.
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "origen.comuna.region",
			"destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findBySolicitanteIdUsuarioOrderByFhAgendaDesc(Long idUsuario);

	// RF04: solicitudes en un estado (p. ej. PENDIENTE).
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "origen.comuna.region",
			"destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findByEstadoNombreEstadoOrderByFhAgenda(String nombreEstado);

	// RF07: viajes del día del Conductor.
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "origen.comuna.region",
			"destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findByConductorIdUsuarioAndFhAgendaBetweenOrderByFhAgenda(
			Long idUsuario, LocalDateTime desde, LocalDateTime hasta);
}
