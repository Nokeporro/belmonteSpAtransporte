package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Solicitante;
import com.belmontespa.transporte.service.SolicitanteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
@RestController
@RequestMapping("/api/v1/solicitantes")
public class SolicitanteController extends CrudController<Solicitante, Long> {
    public SolicitanteController(SolicitanteService service) { super(service); }
    @PutMapping("/{correo}") public Solicitante update(@PathVariable String correo, @RequestBody Solicitante entity) { return service.updateByBusinessKey(correo, entity); }
}
