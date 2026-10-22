package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.SolicitanteDTORequest;
import cl.belmontespa.transporte.dto.response.SolicitanteDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.SolicitanteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// POST es el autorregistro público (RF01); el Administrador solo lee.
@RestController
@RequestMapping("/api/v1/solicitantes")
public class SolicitanteController extends CrudController<SolicitanteDTORequest, SolicitanteDTOResponse> {

	private final SolicitanteService solicitanteService;

	public SolicitanteController(SolicitanteService solicitanteService) {
		this.solicitanteService = solicitanteService;
	}

	@Override
	protected CrudService<?, String, SolicitanteDTORequest, SolicitanteDTOResponse> service() {
		return solicitanteService;
	}

	@Override
	protected void exigirAccesoPrevio(String correo, Authentication auth) {
		if (tieneRol(auth, SOL)) {
			exigirPropio(correo, auth);
		}
	}
}
