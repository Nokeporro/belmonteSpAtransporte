package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.MedioPago;
import com.belmontespa.transporte.repository.MedioPagoRepository;
import org.springframework.stereotype.Service;
@Service
public class MedioPagoService extends CrudService<MedioPago, Long> {
    public MedioPagoService(MedioPagoRepository repository) { super(repository); }
}
