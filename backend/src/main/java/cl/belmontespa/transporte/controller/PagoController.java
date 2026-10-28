package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.CambiarEstadoPagoDTORequest;
import cl.belmontespa.transporte.dto.request.PagoDTORequest;
import cl.belmontespa.transporte.dto.response.PagoDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.PagoService;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController extends CrudController<PagoDTORequest, PagoDTOResponse> {

	private final PagoService pagoService;

	public PagoController(PagoService pagoService) {
		this.pagoService = pagoService;
	}

	@Override
	protected CrudService<?, String, PagoDTORequest, PagoDTOResponse> service() {
		return pagoService;
	}

	@Override
	protected void validarClave(String hash) {
		exigirFormato(Reglas.esHashPago(hash));
	}

	@Override
	protected List<PagoDTOResponse> listarPara(Authentication auth, Map<String, String> filtros) {
		return tieneRol(auth, SOL) ? pagoService.listarDeSolicitante(auth.getName()) : pagoService.listar();
	}

	@Override
	protected void exigirAcceso(String hash, PagoDTOResponse pago, Authentication auth) {
		if (tieneRol(auth, SOL) && pagoService.listarDeSolicitante(auth.getName()).stream()
				.noneMatch(propio -> propio.hash().equals(pago.hash()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	@PostMapping("/{hash}/estado")
	public PagoDTOResponse cambiarEstado(@PathVariable String hash,
			@Valid @RequestBody CambiarEstadoPagoDTORequest request) {
		return pagoService.cambiarEstado(hash, request);
	}
}
