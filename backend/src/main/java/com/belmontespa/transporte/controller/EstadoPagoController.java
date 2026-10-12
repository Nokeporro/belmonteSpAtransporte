package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.EstadoPago;
import com.belmontespa.transporte.service.EstadoPagoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/estados-pago")
public class EstadoPagoController extends CrudController<EstadoPago, Long> {
    public EstadoPagoController(EstadoPagoService service) { super(service); }
}
