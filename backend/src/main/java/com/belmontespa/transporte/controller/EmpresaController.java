package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Empresa;
import com.belmontespa.transporte.service.EmpresaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController extends CrudController<Empresa, Long> {
    public EmpresaController(EmpresaService service) { super(service); }
}
