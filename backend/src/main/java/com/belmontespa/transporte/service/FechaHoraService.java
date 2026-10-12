package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.FechaHora;
import com.belmontespa.transporte.repository.FechaHoraRepository;
import org.springframework.stereotype.Service;
@Service
public class FechaHoraService extends CrudService<FechaHora, Long> {
    public FechaHoraService(FechaHoraRepository repository) { super(repository); }
}
