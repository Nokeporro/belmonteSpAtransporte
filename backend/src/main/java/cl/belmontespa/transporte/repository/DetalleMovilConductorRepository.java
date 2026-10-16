package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.DetalleMovilConductor;
import cl.belmontespa.transporte.model.DetalleMovilConductorId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleMovilConductorRepository
		extends JpaRepository<DetalleMovilConductor, DetalleMovilConductorId> {

	Optional<DetalleMovilConductor> findByMovilPatenteAndConductorUsuarioCorreoIgnoreCase(
			String patente, String correo);

	// CU-04: vehículos habilitados del conductor.
	List<DetalleMovilConductor> findByIdUsuario(Long idUsuario);
}
