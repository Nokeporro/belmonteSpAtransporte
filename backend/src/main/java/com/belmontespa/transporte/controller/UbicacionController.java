package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Ubicacion;
import com.belmontespa.transporte.service.UbicacionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/ubicaciones")
public class UbicacionController extends CrudController<Ubicacion, Long> {
    public UbicacionController(UbicacionService service) { super(service); }
}
