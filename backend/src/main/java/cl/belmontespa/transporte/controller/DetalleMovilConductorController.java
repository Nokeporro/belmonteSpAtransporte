package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.DetalleMovilConductorDTORequest;
import cl.belmontespa.transporte.dto.response.DetalleMovilConductorDTOResponse;
import cl.belmontespa.transporte.service.DetalleMovilConductorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/habilitaciones-movil-conductor")
public class DetalleMovilConductorController {

	private final DetalleMovilConductorService detalleService;

	public DetalleMovilConductorController(DetalleMovilConductorService detalleService) {
		this.detalleService = detalleService;
	}

	@GetMapping
	public List<DetalleMovilConductorDTOResponse> listar(@RequestParam(required = false) String patente,
			@RequestParam(required = false) String correo) {
		return detalleService.listar(patente, correo);
	}

	@GetMapping("/{patente}/{correo}")
	public DetalleMovilConductorDTOResponse obtener(@PathVariable String patente, @PathVariable String correo) {
		return detalleService.obtener(new DetalleMovilConductorService.Clave(patente, correo));
	}

	@PostMapping
	public ResponseEntity<DetalleMovilConductorDTOResponse> crear(@Valid @RequestBody DetalleMovilConductorDTORequest request) {
		var alta = detalleService.habilitar(request);
		return ResponseEntity.status(alta.creada() ? HttpStatus.CREATED : HttpStatus.OK).body(alta.habilitacion());
	}
}
