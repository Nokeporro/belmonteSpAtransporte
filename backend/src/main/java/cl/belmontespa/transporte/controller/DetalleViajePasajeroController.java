package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.DetalleViajePasajeroDTORequest;
import cl.belmontespa.transporte.dto.response.DetalleViajePasajeroDTOResponse;
import cl.belmontespa.transporte.service.DetalleViajePasajeroService;
import cl.belmontespa.transporte.service.ViajeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Pasajeros de un viaje (CU-01). Clave de dos segmentos: no extiende CrudController.
@RestController
@RequestMapping("/api/v1/pasajeros-viaje")
public class DetalleViajePasajeroController {

	private final DetalleViajePasajeroService detalleService;
	private final ViajeService viajeService;

	public DetalleViajePasajeroController(DetalleViajePasajeroService detalleService, ViajeService viajeService) {
		this.detalleService = detalleService;
		this.viajeService = viajeService;
	}

	@GetMapping
	public List<DetalleViajePasajeroDTOResponse> listar(Authentication auth) {
		return detalleService.listar().stream().filter(d -> esAdmin(auth)
				|| viajeService.obtener(d.folio()).correoSolicitante().equalsIgnoreCase(auth.getName())).toList();
	}

	@GetMapping("/{folio}/{dniHash}")
	public DetalleViajePasajeroDTOResponse obtener(@PathVariable String folio, @PathVariable String dniHash, Authentication auth) {
		exigirAcceso(folio, auth);
		return detalleService.obtener(new DetalleViajePasajeroService.Clave(folio, dniHash));
	}

	// Solo el Solicitante dueño del viaje agrega pasajeros.
	@PostMapping
	public org.springframework.http.ResponseEntity<DetalleViajePasajeroDTOResponse> crear(@Valid @RequestBody DetalleViajePasajeroDTORequest request,
			Authentication auth) {
		String dueno = viajeService.obtener(request.folio()).correoSolicitante();
		if (!dueno.equalsIgnoreCase(auth.getName())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		boolean existente = detalleService.existe(request.folio(), request.dniHash());
		return org.springframework.http.ResponseEntity.status(existente ? HttpStatus.OK : HttpStatus.CREATED).body(detalleService.crear(request));
	}

	private boolean esAdmin(Authentication auth) {
		return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
	}

	private void exigirAcceso(String folio, Authentication auth) {
		if (!esAdmin(auth) && !viajeService.obtener(folio).correoSolicitante().equalsIgnoreCase(auth.getName())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}
}
