package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Pago;
import com.belmontespa.transporte.repository.PagoRepository;
import org.springframework.stereotype.Service;
@Service
public class PagoService extends CrudService<Pago, Long> {
    public PagoService(PagoRepository repository) { super(repository); }
}
