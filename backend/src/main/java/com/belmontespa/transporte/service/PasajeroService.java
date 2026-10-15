package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Pasajero;
import com.belmontespa.transporte.repository.PasajeroRepository;
import org.springframework.stereotype.Service;
@Service
public class PasajeroService extends CrudService<Pasajero, Long> {
    public PasajeroService(PasajeroRepository repository) { super(repository); }
}
