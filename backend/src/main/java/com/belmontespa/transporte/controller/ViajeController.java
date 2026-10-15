package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Viaje;
import com.belmontespa.transporte.service.ViajeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/viajes")
public class ViajeController extends CrudController<Viaje, Long> {
    public ViajeController(ViajeService service) { super(service); }
}
