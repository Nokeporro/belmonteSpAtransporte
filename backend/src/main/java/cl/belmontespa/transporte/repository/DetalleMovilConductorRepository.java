package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.DetalleMovilConductor;
import cl.belmontespa.transporte.model.DetalleMovilConductorId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface DetalleMovilConductorRepository
		extends JpaRepository<DetalleMovilConductor, DetalleMovilConductorId> {

	Optional<DetalleMovilConductor> findByMovilPatenteAndConductorUsuarioCorreoIgnoreCase(
			String patente, String correo);

	@EntityGraph(attributePaths = {"movil.estadoMovil", "conductor.usuario", "conductor.estadoConductor"})
	@Query("""
			select d from DetalleMovilConductor d
			where (:patente is null or d.movil.patente = :patente)
			and (:correo is null or lower(d.conductor.usuario.correo) = :correo)
			order by d.movil.patente, d.conductor.usuario.nombre, d.conductor.usuario.correo
			""")
	List<DetalleMovilConductor> filtrar(String patente, String correo);

	List<DetalleMovilConductor> findByIdUsuario(Long idUsuario);
}
