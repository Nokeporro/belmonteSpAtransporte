package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.MovilDTORequest;
import cl.belmontespa.transporte.dto.response.MovilDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.MovilService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/moviles")
public class MovilController extends CrudController<MovilDTORequest, MovilDTOResponse> {

	private final MovilService movilService;

	public MovilController(MovilService movilService) {
		this.movilService = movilService;
	}

	@Override
	protected CrudService<?, String, MovilDTORequest, MovilDTOResponse> service() {
		return movilService;
	}
}
