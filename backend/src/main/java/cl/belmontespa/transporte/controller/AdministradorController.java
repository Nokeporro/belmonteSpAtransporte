package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.AdministradorDTORequest;
import cl.belmontespa.transporte.dto.response.AdministradorDTOResponse;
import cl.belmontespa.transporte.service.AdministradorService;
import cl.belmontespa.transporte.service.CrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/administradores")
public class AdministradorController extends CrudController<AdministradorDTORequest, AdministradorDTOResponse> {

	private final AdministradorService administradorService;

	public AdministradorController(AdministradorService administradorService) {
		this.administradorService = administradorService;
	}

	@Override
	protected CrudService<?, String, AdministradorDTORequest, AdministradorDTOResponse> service() {
		return administradorService;
	}
}
