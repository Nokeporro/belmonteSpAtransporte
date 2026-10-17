package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.HistorialViaje;
import java.util.List;
import org.springframework.data.repository.Repository;

// Solo lectura: HISTORIAL_VIAJE lo escriben los triggers de VIAJE, sin save/delete.
public interface HistorialViajeRepository extends Repository<HistorialViaje, Long> {

	List<HistorialViaje> findByViajeIdViajeOrderByFechaCambio(Long idViaje);
}
