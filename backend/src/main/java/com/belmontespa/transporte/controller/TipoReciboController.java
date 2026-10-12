package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.TipoRecibo;
import com.belmontespa.transporte.service.TipoReciboService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/tipos-recibo")
public class TipoReciboController extends CrudController<TipoRecibo, Long> {
    public TipoReciboController(TipoReciboService service) { super(service); }
}
