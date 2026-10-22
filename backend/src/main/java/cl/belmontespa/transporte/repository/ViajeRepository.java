package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Viaje;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

// Los finders cargan de una vez lo que lee ViajeDTOResponse.from() (open-in-view=false). No
// ordenan ni filtran por fecha: el orden de las listas (fhAgenda y folio numérico) lo aplica
// OrdenViajes y los filtros de GET /viajes, ViajeService.
public interface ViajeRepository extends JpaRepository<Viaje, Long> {

	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	Optional<Viaje> findByFolio(String folio);

	// RF03: historial del Solicitante.
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findBySolicitanteIdUsuario(Long idUsuario);

	// RF04: solicitudes en un estado (p. ej. PENDIENTE).
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findByEstadoNombreEstado(String nombreEstado);

	// RF07: viajes asignados al Conductor, de cualquier día.
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findByConductorIdUsuario(Long idUsuario);

	// V2: viajes del móvil, para comprobar que ninguno quede a menos de 2 horas de otro.
	@EntityGraph(attributePaths = {"estado"})
	List<Viaje> findByMovilIdMovil(Long idMovil);

	// Todos los viajes (Administrador sin estado).
	@Override
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findAll();

	// Los pagos cuelgan del viaje (viaje.pago_id): de aquí sale la lista de pagos con su folio.
	@EntityGraph(attributePaths = {"pago.estadoPago"})
	List<Viaje> findByPagoIsNotNull();
}
