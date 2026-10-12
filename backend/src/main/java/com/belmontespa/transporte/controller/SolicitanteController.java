package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Solicitante;
import com.belmontespa.transporte.service.SolicitanteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/solicitantes")
public class SolicitanteController extends CrudController<Solicitante, Long> {
    public SolicitanteController(SolicitanteService service) { super(service); }
}
