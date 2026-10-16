package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.EmpresaDTORequest;
import cl.belmontespa.transporte.dto.response.EmpresaDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.EmpresaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController extends CrudController<EmpresaDTORequest, EmpresaDTOResponse> {

	private final EmpresaService empresaService;

	public EmpresaController(EmpresaService empresaService) {
		this.empresaService = empresaService;
	}

	@Override
	protected CrudService<?, String, EmpresaDTORequest, EmpresaDTOResponse> service() {
		return empresaService;
	}
}
