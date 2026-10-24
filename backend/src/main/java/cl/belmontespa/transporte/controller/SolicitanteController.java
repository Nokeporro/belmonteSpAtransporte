package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.SolicitanteDTORequest;
import cl.belmontespa.transporte.dto.response.SolicitanteDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.SolicitanteService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Autorregistro público (RF01); solo el Administrador lista y revisa cuentas pendientes (CU-17).
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
	protected List<SolicitanteDTOResponse> listarPara(Authentication auth, Map<String, String> filtros) {
		return solicitanteService.listarConEstado(filtros.get("estado"));
	}

	@PostMapping("/{correo}/aprobar")
	public SolicitanteDTOResponse aprobar(@PathVariable String correo) {
		return solicitanteService.revisar(correo, true);
	}

	@PostMapping("/{correo}/rechazar")
	public SolicitanteDTOResponse rechazar(@PathVariable String correo) {
		return solicitanteService.revisar(correo, false);
	}

	@Override
	protected void exigirAccesoPrevio(String correo, Authentication auth) {
		if (tieneRol(auth, SOL)) {
			exigirPropio(correo, auth);
		}
	}
}
