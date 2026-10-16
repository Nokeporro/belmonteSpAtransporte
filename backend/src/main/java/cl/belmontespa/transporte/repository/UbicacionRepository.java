package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Comuna;
import cl.belmontespa.transporte.model.Ubicacion;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

	// Find-or-create desde ViajeService: UNIQUE (direccion, id_comuna, latitud, longitud).
	Optional<Ubicacion> findByDireccionAndComunaAndLatitudAndLongitud(
			String direccion, Comuna comuna, BigDecimal latitud, BigDecimal longitud);
}
