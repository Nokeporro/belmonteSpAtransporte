package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.DetalleMovilConductorDTORequest;
import cl.belmontespa.transporte.dto.response.DetalleMovilConductorDTOResponse;
import cl.belmontespa.transporte.model.Conductor;
import cl.belmontespa.transporte.model.DetalleMovilConductor;
import cl.belmontespa.transporte.model.DetalleMovilConductorId;
import cl.belmontespa.transporte.model.Movil;
import cl.belmontespa.transporte.repository.DetalleMovilConductorRepository;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Habilitación vehículo↔conductor (CU-10). Ruta: /habilitaciones-movil-conductor/{patente}/{correo}.
@Service
public class DetalleMovilConductorService extends CrudService<DetalleMovilConductor,
		DetalleMovilConductorService.Clave, DetalleMovilConductorDTORequest, DetalleMovilConductorDTOResponse> {

	public record Clave(String patente, String correoConductor) {}

	private final DetalleMovilConductorRepository detalleRepository;
	private final MovilService movilService;
	private final ConductorService conductorService;

	public DetalleMovilConductorService(DetalleMovilConductorRepository detalleRepository,
			MovilService movilService, ConductorService conductorService) {
		this.detalleRepository = detalleRepository;
		this.movilService = movilService;
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

	// CU-10: si la relación ya existe se ignora sin duplicar.
	@Override
	@Transactional
	public DetalleMovilConductorDTOResponse crear(DetalleMovilConductorDTORequest request) {
		return buscar(new Clave(request.patente(), request.correoConductor()))
				.map(this::aResponse)
				.orElseGet(() -> super.crear(request));
	}

	@Override
	protected DetalleMovilConductor nuevaEntidad(DetalleMovilConductorDTORequest request) {
		Movil movil = movilService.buscarOFallar(request.patente());
		Conductor conductor = conductorService.buscarOFallar(request.correoConductor());
		DetalleMovilConductor detalle = new DetalleMovilConductor();
		detalle.setIdMovil(movil.getIdMovil());
		detalle.setIdUsuario(conductor.getIdUsuario());
		detalle.setMovil(movil);
		detalle.setConductor(conductor);
		return detalle;
	}

	@Override
	protected DetalleMovilConductorDTOResponse aResponse(DetalleMovilConductor detalle) {
		return DetalleMovilConductorDTOResponse.from(detalle);
	}
}
