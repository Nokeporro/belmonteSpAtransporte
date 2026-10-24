package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.AdministradorDTORequest;
import cl.belmontespa.transporte.dto.response.AdministradorDTOResponse;
import cl.belmontespa.transporte.model.Administrador;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

// Sin PUT (README, "CRUD REST").
@Service
public class AdministradorService
		extends CrudService<Administrador, String, AdministradorDTORequest, AdministradorDTOResponse> {

	private final AdministradorRepository administradorRepository;
	private final UsuarioService usuarioService;
	private final long horasTemporal;
	private final EntityManager entityManager;

	public AdministradorService(AdministradorRepository administradorRepository, UsuarioService usuarioService,
			@Value("${app.alta-temporal.horas:72}") long horasTemporal, EntityManager entityManager) {
		if (horasTemporal < 1) throw new IllegalArgumentException("ALTA_TEMPORAL_HORAS debe ser mayor que cero.");
		this.administradorRepository = administradorRepository;
		this.usuarioService = usuarioService;
		this.horasTemporal = horasTemporal;
		this.entityManager = entityManager;
	}

	@Override
	protected JpaRepository<Administrador, Long> repositorio() {
		return administradorRepository;
	}

	@Override
	protected Sort orden() {
		return Sort.by("usuario.nombre", "usuario.correo");
	}

	@Override
	protected Optional<Administrador> buscar(String correo) {
		return administradorRepository.findByUsuarioCorreoIgnoreCase(correo);
	}

	@Override
	protected Administrador nuevaEntidad(AdministradorDTORequest request) {
		Administrador administrador = new Administrador();
		administrador.setUsuario(usuarioService.nuevoConTemporal(
				request.nombre(), request.correo(), request.telefono(), request.password(), horasTemporal));
		// fecha_registro la genera MariaDB; el alta devuelve la misma fecha que las consultas.
		entityManager.refresh(administrador.getUsuario());
		return administrador;
	}

	@Override
	protected AdministradorDTOResponse aResponse(Administrador administrador) {
		return AdministradorDTOResponse.from(administrador);
	}
}
