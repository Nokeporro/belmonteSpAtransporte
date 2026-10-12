package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.DetalleViajePasajero;
import com.belmontespa.transporte.model.DetalleViajePasajeroId;
import com.belmontespa.transporte.repository.DetalleViajePasajeroRepository;
import org.springframework.stereotype.Service;
@Service
public class DetalleViajePasajeroService extends CrudService<DetalleViajePasajero, DetalleViajePasajeroId> {
    public DetalleViajePasajeroService(DetalleViajePasajeroRepository repository) { super(repository); }
}
