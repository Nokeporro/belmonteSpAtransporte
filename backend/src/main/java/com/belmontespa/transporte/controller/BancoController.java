package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Banco;
import com.belmontespa.transporte.service.BancoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/bancos")
public class BancoController extends CrudController<Banco, Long> {
    public BancoController(BancoService service) { super(service); }
}
