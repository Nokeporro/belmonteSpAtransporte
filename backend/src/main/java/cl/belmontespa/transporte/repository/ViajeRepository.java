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

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

	boolean existsByConductorIdUsuarioAndEstadoNombreEstadoIn(Long idUsuario, List<String> estados);
	boolean existsByMovilIdMovilAndEstadoNombreEstadoIn(Long idMovil, List<String> estados);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Viaje v where v.folio = :folio")
	Optional<Viaje> bloquearPorFolio(@Param("folio") String folio);

	@Query("select v.folio from Viaje v where v.pago.hash = :hash")
	Optional<String> folioPorHashPago(@Param("hash") String hash);

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

	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findBySolicitanteIdUsuario(Long idUsuario);

	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findByEstadoNombreEstado(String nombreEstado);

	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findByConductorIdUsuario(Long idUsuario);

	@EntityGraph(attributePaths = {"estado"})
	List<Viaje> findByMovilIdMovil(Long idMovil);

	@Override
	@EntityGraph(attributePaths = {"estado", "solicitante.usuario", "solicitante.empresa",
			"origen.comuna.region", "destino.comuna.region", "conductor.usuario", "movil", "pago.estadoPago"})
	List<Viaje> findAll();

	@EntityGraph(attributePaths = {"pago.estadoPago"})
	List<Viaje> findByPagoIsNotNull();

	@Query("select v.folio from Viaje v where v.pago.id = :idPago")
	Optional<String> folioDePago(@Param("idPago") Long idPago);
}
