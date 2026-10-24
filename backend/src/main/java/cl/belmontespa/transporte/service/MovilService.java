package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.MovilDTORequest;
import cl.belmontespa.transporte.dto.response.MovilDTOResponse;
import cl.belmontespa.transporte.model.EstadoMovil;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.repository.EstadoMovilRepository;
import cl.belmontespa.transporte.repository.MovilRepository;
import cl.belmontespa.transporte.repository.ViajeRepository;
import java.util.List;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MovilService extends CrudService<Movil, String, MovilDTORequest, MovilDTOResponse> {

	static final String DISPONIBLE = "DISPONIBLE";
	static final String EN_USO = "EN_USO";

	private static final int BYTES_CODIGO_ABORDAJE = 8;

	private final MovilRepository movilRepository;
	private final EstadoMovilRepository estadoMovilRepository;
	private final ViajeRepository viajeRepository;
	private final SecureRandom random = new SecureRandom();

	public MovilService(MovilRepository movilRepository, EstadoMovilRepository estadoMovilRepository,
			ViajeRepository viajeRepository) {
		this.movilRepository = movilRepository;
		this.estadoMovilRepository = estadoMovilRepository;
		this.viajeRepository = viajeRepository;
	}

	@Override
	protected JpaRepository<Movil, Long> repositorio() {
		return movilRepository;
	}

	@Override
	protected Sort orden() {
		return Sort.by("patente");
	}

	@Override
	protected Optional<Movil> buscar(String patente) {
		return movilRepository.findByPatente(patente);
	}

	// CU-09: el vehículo queda Disponible y con el código del QR de abordaje.
	@Override
	protected Movil nuevaEntidad(MovilDTORequest request) {
		if (movilRepository.findByPatente(request.patente()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La patente ya está registrada");
		}
		Movil movil = new Movil();
		movil.setPatente(request.patente());
		movil.setCapacidad(request.capacidad());
		movil.setGpsSerial(request.gpsSerial());
		movil.setEstadoMovil(estado(DISPONIBLE));
		movil.setCodigoAbordaje(codigoAbordaje());
		return movil;
	}

	@Override
	protected MovilDTOResponse aResponse(Movil movil) {
		return MovilDTOResponse.from(movil);
	}

	void cambiarEstado(Movil movil, String nombreEstado) {
		movil.setEstadoMovil(estado(nombreEstado));
	}

	Movil bloquear(String patente) {
		return movilRepository.bloquearPorPatente(patente)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@Transactional
	public MovilDTOResponse cambiarActivo(String patente, boolean activo) {
		Movil movil = bloquear(patente);
		if (movil.isActivo() != activo) {
			if (!activo && viajeRepository.existsByMovilIdMovilAndEstadoNombreEstadoIn(
					movil.getIdMovil(), List.of("ASIGNADO", "EN_CURSO"))) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "El vehículo tiene viajes asignados o en curso.");
			}
			movil.setActivo(activo);
		}
		return aResponse(movil);
	}

	// 8 bytes de SecureRandom como 16 hexadecimales en mayúsculas.
	private String codigoAbordaje() {
		byte[] bytes = new byte[BYTES_CODIGO_ABORDAJE];
		random.nextBytes(bytes);
		return HexFormat.of().withUpperCase().formatHex(bytes);
	}

	private EstadoMovil estado(String nombre) {
		return estadoMovilRepository.findByNombre(nombre)
				.orElseThrow(() -> new IllegalStateException("Falta el estado de móvil " + nombre));
	}
}
