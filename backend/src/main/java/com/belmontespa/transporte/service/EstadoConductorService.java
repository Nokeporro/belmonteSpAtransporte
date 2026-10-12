package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.EstadoConductor;
import com.belmontespa.transporte.repository.EstadoConductorRepository;
import org.springframework.stereotype.Service;
@Service
public class EstadoConductorService extends CrudService<EstadoConductor, Long> {
    public EstadoConductorService(EstadoConductorRepository repository) { super(repository); }
}
