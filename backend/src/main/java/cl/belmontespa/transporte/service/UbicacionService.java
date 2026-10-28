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

@Service
public class UbicacionService {

	private final UbicacionRepository ubicacionRepository;
	private final ComunaRepository comunaRepository;

	public UbicacionService(UbicacionRepository ubicacionRepository, ComunaRepository comunaRepository) {
		this.ubicacionRepository = ubicacionRepository;
		this.comunaRepository = comunaRepository;
	}

	Ubicacion obtenerOCrear(ViajeDTORequest.Ubicacion datos, String campo) {
		Comuna comuna = comunaRepository.findAll().stream()
				.filter(c -> normalizar(c.getNombre()).equals(normalizar(datos.comuna())))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						campo + ".comuna: No existe en el catálogo."));
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

	static String normalizar(String texto) {
		return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).trim()
				.replaceFirst("^region(?: de la| del| de)?\\s+", "").replaceAll("\\s+", " ");
	}
}
