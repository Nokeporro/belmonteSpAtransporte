package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.PasajeroDTORequest;
import cl.belmontespa.transporte.dto.response.PasajeroDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.PasajeroService;
import cl.belmontespa.transporte.validation.Reglas;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pasajeros")
public class PasajeroController extends CrudController<PasajeroDTORequest, PasajeroDTOResponse> {

	private final PasajeroService pasajeroService;

	public PasajeroController(PasajeroService pasajeroService) {
		this.pasajeroService = pasajeroService;
	}

	@Override
	protected CrudService<?, String, PasajeroDTORequest, PasajeroDTOResponse> service() {
		return pasajeroService;
	}

	@Override
	protected void validarClave(String dniHash) {
		exigirFormato(Reglas.esDniHash(dniHash));
	}
}
