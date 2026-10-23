package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.AbordajeDTORequest;
import cl.belmontespa.transporte.dto.response.AbordajeDTOResponse;
import cl.belmontespa.transporte.model.DetalleViajePasajero;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.model.Viaje;
import cl.belmontespa.transporte.repository.DetalleViajePasajeroRepository;
import cl.belmontespa.transporte.repository.MovilRepository;
import cl.belmontespa.transporte.repository.ViajeRepository;
import cl.belmontespa.transporte.validation.Documento;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// CU-19/RF28: el Pasajero marca su abordaje con el QR del vehículo y su documento, sin cuenta (README, "Abordaje").
@Service
public class AbordajeService {

	// Un único texto para todo lo que no encuentra viaje: no revela si falló la patente, el código o el documento (RNF08).
	static final String NO_ENCONTRADO = "No encontramos un viaje para este documento en este vehículo";

	private final MovilRepository movilRepository;
	private final ViajeRepository viajeRepository;
	private final DetalleViajePasajeroRepository detalleViajePasajeroRepository;
	private final PasajeroService pasajeroService;
	private final Clock clock;

	public AbordajeService(MovilRepository movilRepository, ViajeRepository viajeRepository,
			DetalleViajePasajeroRepository detalleViajePasajeroRepository, PasajeroService pasajeroService, Clock clock) {
		this.movilRepository = movilRepository;
		this.viajeRepository = viajeRepository;
		this.detalleViajePasajeroRepository = detalleViajePasajeroRepository;
		this.pasajeroService = pasajeroService;
		this.clock = clock;
	}

	// Los candidatos son los viajes del vehículo EN_CURSO o ASIGNADOS dentro de la ventana de V3; el dni_hash elige
	// entre ellos y, si calza en varios, gana el de fh_agenda más cercana a ahora. No cambia ningún otro estado.
	@Transactional
	public AbordajeDTOResponse registrar(AbordajeDTORequest request) {
		Documento.normalizar(request.tipoDocumento(), request.dni());
		Movil movil = movilRepository.findByPatente(request.patente())
				.filter(m -> codigoCoincide(m, request.codigoAbordaje()))
				.orElseThrow(AbordajeService::noEncontrado);
		String dniHash = pasajeroService.hashDni(request.dni());
		// La base guarda segundos: truncar aquí hace que la primera respuesta y las repeticiones coincidan.
		LocalDateTime ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
		DetalleViajePasajero detalle = viajeRepository.findByMovilIdMovil(movil.getIdMovil()).stream()
				.filter(viaje -> esCandidato(viaje, ahora))
				.sorted(Comparator.comparing((Viaje viaje) -> Duration.between(ahora, viaje.getFhAgenda()).abs())
						.thenComparing(Viaje::getIdViaje))
				.map(viaje -> detalleViajePasajeroRepository.findByViajeFolioAndPasajeroDniHash(viaje.getFolio(), dniHash))
				.flatMap(Optional::stream)
				.findFirst()
				.orElseThrow(AbordajeService::noEncontrado);
		if (detalle.getFhAbordaje() == null) {
			detalle.setFhAbordaje(ahora);
		}
		return new AbordajeDTOResponse(detalle.getFhAbordaje());
	}

	private static boolean esCandidato(Viaje viaje, LocalDateTime ahora) {
		String estado = viaje.getEstado().getNombreEstado();
		return ViajeService.EN_CURSO.equals(estado)
				|| ViajeService.ASIGNADO.equals(estado) && ViajeService.dentroDeLaVentanaDeInicio(viaje.getFhAgenda(), ahora);
	}

	// Tiempo constante: el código del QR es un secreto compartido.
	private static boolean codigoCoincide(Movil movil, String codigo) {
		return MessageDigest.isEqual(movil.getCodigoAbordaje().getBytes(StandardCharsets.UTF_8),
				codigo.getBytes(StandardCharsets.UTF_8));
	}

	private static ResponseStatusException noEncontrado() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, NO_ENCONTRADO);
	}
}
