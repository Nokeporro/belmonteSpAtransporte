package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.ViajeDTORequest;
import cl.belmontespa.transporte.model.Comuna;
import cl.belmontespa.transporte.model.Ubicacion;
import cl.belmontespa.transporte.repository.ComunaRepository;
import cl.belmontespa.transporte.repository.UbicacionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Sin recurso propio: solo la usa ViajeService para origen y destino (README).
@Service
public class UbicacionService {

	private final UbicacionRepository ubicacionRepository;
	private final ComunaRepository comunaRepository;

	public UbicacionService(UbicacionRepository ubicacionRepository, ComunaRepository comunaRepository) {
		this.ubicacionRepository = ubicacionRepository;
		this.comunaRepository = comunaRepository;
	}

	// Reutiliza la ubicación si ya existe (UNIQUE direccion + comuna + coordenadas, RF10).
	Ubicacion obtenerOCrear(ViajeDTORequest.Ubicacion datos) {
		Comuna comuna = comunaRepository.findByNombreAndRegionNombre(datos.comuna(), datos.region())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"La comuna " + datos.comuna() + " no existe en la región " + datos.region()));
		BigDecimal latitud = datos.latitud().setScale(6, RoundingMode.HALF_UP);
		BigDecimal longitud = datos.longitud().setScale(6, RoundingMode.HALF_UP);
		return ubicacionRepository.findByDireccionAndComunaAndLatitudAndLongitud(
				datos.direccion(), comuna, latitud, longitud)
				.orElseGet(() -> {
					Ubicacion ubicacion = new Ubicacion();
					ubicacion.setDireccion(datos.direccion());
					ubicacion.setComuna(comuna);
					ubicacion.setLatitud(latitud);
					ubicacion.setLongitud(longitud);
					return ubicacionRepository.save(ubicacion);
				});
	}
}
