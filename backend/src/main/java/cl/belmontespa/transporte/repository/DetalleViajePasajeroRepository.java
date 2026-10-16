package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.DetalleViajePasajero;
import cl.belmontespa.transporte.model.DetalleViajePasajeroId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleViajePasajeroRepository
		extends JpaRepository<DetalleViajePasajero, DetalleViajePasajeroId> {

	Optional<DetalleViajePasajero> findByViajeFolioAndPasajeroDniHash(String folio, String dniHash);

	// Pasajeros de un viaje, para ViajeDTOResponse.
	List<DetalleViajePasajero> findByIdViaje(Long idViaje);
}
