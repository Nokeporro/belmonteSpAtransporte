package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Viaje;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

// Los finders cargan de una vez lo que lee ViajeDTOResponse.from() (open-in-view=false). No
// ordenan ni filtran por fecha: el orden de las listas (fhAgenda y folio numérico) lo aplica
// OrdenViajes y los filtros de GET /viajes, ViajeService.
public interface ViajeRepository extends JpaRepository<Viaje, Long> {

	boolean existsByConductorIdUsuarioAndEstadoNombreEstadoIn(Long idUsuario, List<String> estados);
	boolean existsByMovilIdMovilAndEstadoNombreEstadoIn(Long idMovil, List<String> estados);

	// V5: serializa dos cancelaciones del mismo viaje dentro de su transacción.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Viaje v where v.folio = :folio")
	Optional<Viaje> bloquearPorFolio(@Param("folio") String folio);

	// Solo identifica el viaje, sin cargar entidades ni bloquear el pago antes que el viaje.
	@Query("select v.folio from Viaje v where v.pago.hash = :hash")
	Optional<String> folioPorHashPago(@Param("hash") String hash);

	// HU15: una consulta para el resumen de toda la lista, sin cargar viajes ni pasajeros.
	interface ResumenConductor {
		Long getIdConductor();
		Long getViajesAsignados();
		String getFolioEnCurso();
	}

	@Query("""
			select v.conductor.idUsuario as idConductor,
			       sum(case when v.estado.nombreEstado = 'ASIGNADO' then 1 else 0 end) as viajesAsignados,
			       min(case when v.estado.nombreEstado = 'EN_CURSO' then v.folio else null end) as folioEnCurso
			from Viaje v
			where v.conductor.idUsuario in :conductores
			  and v.estado.nombreEstado in ('ASIGNADO', 'EN_CURSO')
			group by v.conductor.idUsuario
			""")
	List<ResumenConductor> resumirConductores(@Param("conductores") List<Long> conductores);

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

	@Query("select v.folio from Viaje v where v.pago.id = :idPago")
	Optional<String> folioDePago(@Param("idPago") Long idPago);
}
