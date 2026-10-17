package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.DetalleMovilConductorDTORequest;
import cl.belmontespa.transporte.dto.response.DetalleMovilConductorDTOResponse;
import cl.belmontespa.transporte.service.DetalleMovilConductorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Habilitación vehículo↔conductor (CU-10). Clave de dos segmentos: no extiende CrudController.
@RestController
@RequestMapping("/api/v1/habilitaciones-movil-conductor")
public class DetalleMovilConductorController {

	private final DetalleMovilConductorService detalleService;

	public DetalleMovilConductorController(DetalleMovilConductorService detalleService) {
		this.detalleService = detalleService;
	}

	@GetMapping
	public List<DetalleMovilConductorDTOResponse> listar() {
		return detalleService.listar();
	}

	@GetMapping("/{patente}/{correo}")
	public DetalleMovilConductorDTOResponse obtener(@PathVariable String patente, @PathVariable String correo) {
		return detalleService.obtener(new DetalleMovilConductorService.Clave(patente, correo));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DetalleMovilConductorDTOResponse crear(@Valid @RequestBody DetalleMovilConductorDTORequest request) {
		return detalleService.crear(request);
	}
}
