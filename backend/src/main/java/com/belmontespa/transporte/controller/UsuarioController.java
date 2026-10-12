package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Usuario;
import com.belmontespa.transporte.service.UsuarioService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController extends CrudController<Usuario, Long> {
    public UsuarioController(UsuarioService service) { super(service); }
}
