package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.service.DetalleViajePasajeroService;
import com.belmontespa.transporte.model.DetalleViajePasajero;
import com.belmontespa.transporte.model.DetalleViajePasajeroId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/pasajeros-viaje")
public class DetalleViajePasajeroController {
    private final DetalleViajePasajeroService service;
    public DetalleViajePasajeroController(DetalleViajePasajeroService service) { this.service = service; }
    @GetMapping public List<DetalleViajePasajero> findAll() { return service.findAll(); }
    @GetMapping("/{viajeId}/{pasajeroId}") public DetalleViajePasajero findById(@PathVariable Long viajeId, @PathVariable Long pasajeroId) { return service.findById(new DetalleViajePasajeroId(viajeId, pasajeroId)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public DetalleViajePasajero create(@RequestBody DetalleViajePasajero entity) { return service.create(entity); }
    @DeleteMapping("/{viajeId}/{pasajeroId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long viajeId, @PathVariable Long pasajeroId) { service.delete(new DetalleViajePasajeroId(viajeId, pasajeroId)); }
}
