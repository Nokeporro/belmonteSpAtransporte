package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.MedioPago;
import com.belmontespa.transporte.service.MedioPagoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/medios-pago")
public class MedioPagoController extends CrudController<MedioPago, Long> {
    public MedioPagoController(MedioPagoService service) { super(service); }
}
