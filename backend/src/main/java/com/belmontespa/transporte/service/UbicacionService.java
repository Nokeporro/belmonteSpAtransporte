package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Ubicacion;
import com.belmontespa.transporte.repository.UbicacionRepository;
import org.springframework.stereotype.Service;
@Service
public class UbicacionService extends CrudService<Ubicacion, Long> {
    public UbicacionService(UbicacionRepository repository) { super(repository); }
}
