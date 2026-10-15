package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Viaje;
import com.belmontespa.transporte.repository.ViajeRepository;
import org.springframework.stereotype.Service;
@Service
public class ViajeService extends CrudService<Viaje, Long> {
    public ViajeService(ViajeRepository repository) { super(repository); }
}
