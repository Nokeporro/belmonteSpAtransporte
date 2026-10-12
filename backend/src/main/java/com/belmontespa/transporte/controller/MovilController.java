package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Movil;
import com.belmontespa.transporte.service.MovilService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/moviles")
public class MovilController extends CrudController<Movil, Long> {
    public MovilController(MovilService service) { super(service); }
}
