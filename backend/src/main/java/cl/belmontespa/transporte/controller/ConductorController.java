package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.ConductorDTORequest;
import cl.belmontespa.transporte.dto.response.ConductorDTOResponse;
import cl.belmontespa.transporte.service.ConductorService;
import cl.belmontespa.transporte.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// El Administrador crea y edita conductores (CU-07); el Conductor solo consulta su cuenta.
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
	protected void exigirAcceso(String correo, ConductorDTOResponse conductor, Authentication auth) {
		if (tieneRol(auth, CON)) {
			exigirPropio(correo, auth);
		}
	}

	// Solo Administrador (SecurityConfig): reemplaza datos y contraseña (CU-07/RF14).
	@PutMapping("/{clave}")
	public ConductorDTOResponse actualizar(@PathVariable String clave, @Valid @RequestBody ConductorDTORequest request) {
		return conductorService.actualizar(clave, request);
	}
}
