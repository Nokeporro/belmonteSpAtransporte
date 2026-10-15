package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.EstadoPago;
import com.belmontespa.transporte.repository.EstadoPagoRepository;
import org.springframework.stereotype.Service;
@Service
public class EstadoPagoService extends CrudService<EstadoPago, Long> {
    public EstadoPagoService(EstadoPagoRepository repository) { super(repository); }
}
