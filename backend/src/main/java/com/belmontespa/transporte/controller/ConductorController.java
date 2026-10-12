package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Conductor;
import com.belmontespa.transporte.service.ConductorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/conductores")
public class ConductorController extends CrudController<Conductor, Long> {
    public ConductorController(ConductorService service) { super(service); }
}
