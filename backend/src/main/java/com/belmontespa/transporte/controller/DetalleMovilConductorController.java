package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.service.DetalleMovilConductorService;
import com.belmontespa.transporte.model.DetalleMovilConductor;
import com.belmontespa.transporte.model.DetalleMovilConductorId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/habilitaciones-movil-conductor")
public class DetalleMovilConductorController {
    private final DetalleMovilConductorService service;
    public DetalleMovilConductorController(DetalleMovilConductorService service) { this.service = service; }
    @GetMapping public List<DetalleMovilConductor> findAll() { return service.findAll(); }
    @GetMapping("/{movilId}/{conductorId}") public DetalleMovilConductor findById(@PathVariable Long movilId, @PathVariable Long conductorId) { return service.findById(new DetalleMovilConductorId(movilId, conductorId)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public DetalleMovilConductor create(@RequestBody DetalleMovilConductor entity) { return service.create(entity); }
}
