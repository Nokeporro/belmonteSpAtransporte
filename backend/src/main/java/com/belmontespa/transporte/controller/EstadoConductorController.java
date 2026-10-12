package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.EstadoConductor;
import com.belmontespa.transporte.service.EstadoConductorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/estados-conductor")
public class EstadoConductorController extends CrudController<EstadoConductor, Long> {
    public EstadoConductorController(EstadoConductorService service) { super(service); }
}
