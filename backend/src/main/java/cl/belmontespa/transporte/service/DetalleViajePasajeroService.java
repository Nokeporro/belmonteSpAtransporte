package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.DetalleViajePasajeroDTORequest;
import cl.belmontespa.transporte.dto.response.DetalleViajePasajeroDTOResponse;
import cl.belmontespa.transporte.model.DetalleViajePasajero;
import cl.belmontespa.transporte.model.DetalleViajePasajeroId;
import cl.belmontespa.transporte.model.Pasajero;
import cl.belmontespa.transporte.model.Viaje;
import cl.belmontespa.transporte.repository.DetalleViajePasajeroRepository;
import cl.belmontespa.transporte.repository.ViajeRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Pasajeros de un viaje. Ruta: /pasajeros-viaje/{folio}/{dni_hash}. Cada alta suma a
// VIAJE.cantidad_pasajeros por trigger.
@Service
public class DetalleViajePasajeroService extends CrudService<DetalleViajePasajero,
		DetalleViajePasajeroService.Clave, DetalleViajePasajeroDTORequest, DetalleViajePasajeroDTOResponse> {

	public record Clave(String folio, String dniHash) {}

	private final DetalleViajePasajeroRepository detalleRepository;
	private final ViajeRepository viajeRepository;
	private final PasajeroService pasajeroService;

	public DetalleViajePasajeroService(DetalleViajePasajeroRepository detalleRepository,
			ViajeRepository viajeRepository, PasajeroService pasajeroService) {
		this.detalleRepository = detalleRepository;
		this.viajeRepository = viajeRepository;
		this.pasajeroService = pasajeroService;
	}

	@Override
	protected JpaRepository<DetalleViajePasajero, DetalleViajePasajeroId> repositorio() {
		return detalleRepository;
	}

	@Override
	protected Optional<DetalleViajePasajero> buscar(Clave clave) {
		return detalleRepository.findByViajeFolioAndPasajeroDniHash(clave.folio(), clave.dniHash());
	}

	// Si el pasajero ya está en el viaje no se duplica.
	@Transactional(readOnly = true)
	public boolean existe(String folio, String dniHash) {
		return buscar(new Clave(folio, dniHash)).isPresent();
	}

	@Override
	@Transactional
	public DetalleViajePasajeroDTOResponse crear(DetalleViajePasajeroDTORequest request) {
		return buscar(new Clave(request.folio(), request.dniHash()))
				.map(this::aResponse)
				.orElseGet(() -> super.crear(request));
	}

	@Override
	protected DetalleViajePasajero nuevaEntidad(DetalleViajePasajeroDTORequest request) {
		Viaje viaje = viajeRepository.findByFolio(request.folio())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El viaje no existe"));
		String estado = viaje.getEstado().getNombreEstado();
		if (!ViajeService.PENDIENTE.equals(estado) && !ViajeService.ASIGNADO.equals(estado)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Solo se agregan pasajeros a viajes Pendientes o Asignados");
		}
		// El trigger también lo impide; aquí se responde 409 en vez de un error SQL.
		if (viaje.getMovil() != null
				&& Objects.requireNonNullElse(viaje.getCantidadPasajeros(), 0) >= viaje.getMovil().getCapacidad()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El viaje ya alcanzó la capacidad del móvil");
		}
		Pasajero pasajero = pasajeroService.buscarOFallar(request.dniHash());
		DetalleViajePasajero detalle = new DetalleViajePasajero();
		detalle.setIdViaje(viaje.getIdViaje());
		detalle.setIdPasajero(pasajero.getIdPasajero());
		detalle.setViaje(viaje);
		detalle.setPasajero(pasajero);
		return detalle;
	}

	@Override
	protected DetalleViajePasajeroDTOResponse aResponse(DetalleViajePasajero detalle) {
		return DetalleViajePasajeroDTOResponse.from(detalle);
	}
}
