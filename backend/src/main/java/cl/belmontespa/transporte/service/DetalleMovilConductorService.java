package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.DetalleMovilConductorDTORequest;
import cl.belmontespa.transporte.dto.response.DetalleMovilConductorDTOResponse;
import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.DetalleMovilConductor;
import cl.belmontespa.transporte.model.DetalleMovilConductorId;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.repository.DetalleMovilConductorRepository;
import cl.belmontespa.transporte.repository.MovilRepository;
import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Habilitación vehículo↔conductor (CU-10). Ruta: /habilitaciones-movil-conductor/{patente}/{correo}.
@Service
public class DetalleMovilConductorService extends CrudService<DetalleMovilConductor,
		DetalleMovilConductorService.Clave, DetalleMovilConductorDTORequest, DetalleMovilConductorDTOResponse> {

	public record Clave(String patente, String correoConductor) {
		public Clave {
			patente = Normalizar.patente(patente);
			correoConductor = Normalizar.correo(correoConductor);
		}
	}

	public record Alta(DetalleMovilConductorDTOResponse habilitacion, boolean creada) {}

	private final DetalleMovilConductorRepository detalleRepository;
	private final MovilRepository movilRepository;
	private final ConductorService conductorService;

	public DetalleMovilConductorService(DetalleMovilConductorRepository detalleRepository,
			MovilRepository movilRepository, ConductorService conductorService) {
		this.detalleRepository = detalleRepository;
		this.movilRepository = movilRepository;
		this.conductorService = conductorService;
	}

	@Override
	protected JpaRepository<DetalleMovilConductor, DetalleMovilConductorId> repositorio() {
		return detalleRepository;
	}

	@Override
	protected Sort orden() {
		return Sort.by("movil.patente", "conductor.usuario.nombre");
	}

	@Override
	protected Optional<DetalleMovilConductor> buscar(Clave clave) {
		return detalleRepository.findByMovilPatenteAndConductorUsuarioCorreoIgnoreCase(
				clave.patente(), clave.correoConductor());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DetalleMovilConductorDTOResponse> listar() {
		return listar(null, null);
	}

	@Transactional(readOnly = true)
	public List<DetalleMovilConductorDTOResponse> listar(String patente, String correo) {
		String p = patente == null || patente.isBlank() ? null : Normalizar.patente(patente);
		String c = correo == null || correo.isBlank() ? null : Normalizar.correo(correo);
		if (p != null && !p.matches(Reglas.PATENTE)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patente: Formato inválido.");
		}
		if (c != null && (c.length() > 150 || !c.matches(Reglas.CORREO))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "correo: Formato inválido.");
		}
		return detalleRepository.filtrar(p, c).stream().map(this::aResponse).toList();
	}

	@Override
	@Transactional
	public DetalleMovilConductorDTOResponse crear(DetalleMovilConductorDTORequest request) {
		return habilitar(request).habilitacion();
	}

	// Mismo orden que V2: conductor y luego vehículo; HU26 no puede desactivarlos durante el alta.
	@Transactional
	public Alta habilitar(DetalleMovilConductorDTORequest request) {
		Conductor conductor = conductorService.bloquear(request.correoConductor());
		Movil movil = movilRepository.bloquearPorPatente(request.patente())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		var existente = buscar(new Clave(request.patente(), request.correoConductor()));
		if (existente.isPresent()) return new Alta(aResponse(existente.get()), false);
		if (!movil.isActivo() || !conductor.isActivo()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El vehículo y el conductor deben estar activos.");
		}
		DetalleMovilConductor detalle = new DetalleMovilConductor();
		detalle.setIdMovil(movil.getIdMovil());
		detalle.setIdUsuario(conductor.getIdUsuario());
		detalle.setMovil(movil);
		detalle.setConductor(conductor);
		return new Alta(aResponse(detalleRepository.save(detalle)), true);
	}

	@Override
	protected DetalleMovilConductorDTOResponse aResponse(DetalleMovilConductor detalle) {
		return DetalleMovilConductorDTOResponse.from(detalle);
	}
}
