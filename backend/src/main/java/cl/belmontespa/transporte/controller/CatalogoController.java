package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.response.CatalogosDTOResponse;
import cl.belmontespa.transporte.service.CatalogoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogos")
public class CatalogoController {
    private final CatalogoService service;
    public CatalogoController(CatalogoService service) { this.service = service; }
    @GetMapping
    public CatalogosDTOResponse obtener() { return service.obtener(); }
}
