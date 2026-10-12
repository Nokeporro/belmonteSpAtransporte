package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Pasajero;
import com.belmontespa.transporte.service.PasajeroService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/pasajeros")
public class PasajeroController extends CrudController<Pasajero, Long> {
    public PasajeroController(PasajeroService service) { super(service); }
}
