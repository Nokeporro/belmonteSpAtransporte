package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.FechaHora;
import com.belmontespa.transporte.service.FechaHoraService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/fechas-horas")
public class FechaHoraController extends CrudController<FechaHora, Long> {
    public FechaHoraController(FechaHoraService service) { super(service); }
}
