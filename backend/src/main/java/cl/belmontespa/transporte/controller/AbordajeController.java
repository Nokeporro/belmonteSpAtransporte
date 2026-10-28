package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.AbordajeDTORequest;
import cl.belmontespa.transporte.dto.response.AbordajeDTOResponse;
import cl.belmontespa.transporte.service.AbordajeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/abordajes")
public class AbordajeController {

	private final AbordajeService abordajeService;

	public AbordajeController(AbordajeService abordajeService) {
		this.abordajeService = abordajeService;
	}

	@PostMapping
	public AbordajeDTOResponse registrar(@Valid @RequestBody AbordajeDTORequest request) {
		return abordajeService.registrar(request);
	}
}
