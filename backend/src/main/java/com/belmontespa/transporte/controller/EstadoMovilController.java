package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.EstadoMovil;
import com.belmontespa.transporte.service.EstadoMovilService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/estados-movil")
public class EstadoMovilController extends CrudController<EstadoMovil, Long> {
    public EstadoMovilController(EstadoMovilService service) { super(service); }
}
