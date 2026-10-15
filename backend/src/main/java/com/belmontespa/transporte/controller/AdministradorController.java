package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Administrador;
import com.belmontespa.transporte.service.AdministradorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/administradores")
public class AdministradorController extends CrudController<Administrador, Long> {
    public AdministradorController(AdministradorService service) { super(service); }
}
