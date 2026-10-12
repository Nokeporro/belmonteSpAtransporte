package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Comuna;
import com.belmontespa.transporte.service.ComunaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/comunas")
public class ComunaController extends CrudController<Comuna, Long> {
    public ComunaController(ComunaService service) { super(service); }
}
