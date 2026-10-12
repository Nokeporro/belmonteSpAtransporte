package com.belmontespa.transporte.controller;
import com.belmontespa.transporte.model.Region;
import com.belmontespa.transporte.service.RegionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/regiones")
public class RegionController extends CrudController<Region, Long> {
    public RegionController(RegionService service) { super(service); }
}
