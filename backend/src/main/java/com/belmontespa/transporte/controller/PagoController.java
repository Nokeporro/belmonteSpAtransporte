package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Pago;
import com.belmontespa.transporte.service.PagoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController extends CrudController<Pago, Long> {
    public PagoController(PagoService service) { super(service); }
}
