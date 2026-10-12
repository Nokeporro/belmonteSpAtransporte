package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.EstadoSolicitud;
import com.belmontespa.transporte.service.EstadoSolicitudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/estados-solicitud")
public class EstadoSolicitudController extends CrudController<EstadoSolicitud, Long> {
    public EstadoSolicitudController(EstadoSolicitudService service) { super(service); }
}
