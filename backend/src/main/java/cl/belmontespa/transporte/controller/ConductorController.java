package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.ConductorDTORequest;
import cl.belmontespa.transporte.dto.request.ActivoDTORequest;
import cl.belmontespa.transporte.dto.response.ConductorDTOResponse;
import cl.belmontespa.transporte.service.ConductorService;
import cl.belmontespa.transporte.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conductores")
public class ConductorController extends CrudController<ConductorDTORequest, ConductorDTOResponse> {

	private final ConductorService conductorService;

	public ConductorController(ConductorService conductorService) {
		this.conductorService = conductorService;
	}

	@Override
	protected CrudService<?, String, ConductorDTORequest, ConductorDTOResponse> service() {
		return conductorService;
	}

	@Override
	protected void exigirAccesoPrevio(String correo, Authentication auth) {
		if (tieneRol(auth, CON)) {
			exigirPropio(correo, auth);
		}
	}

	@PostMapping("/{correo}/activo")
	public ConductorDTOResponse cambiarActivo(@PathVariable String correo, @Valid @RequestBody ActivoDTORequest request) {
		return conductorService.cambiarActivo(correo, request.activo());
	}

	@PutMapping("/{clave}")
	public ConductorDTOResponse actualizar(@PathVariable String clave, @Valid @RequestBody ConductorDTORequest request) {
		return conductorService.actualizar(clave, request);
	}
}
