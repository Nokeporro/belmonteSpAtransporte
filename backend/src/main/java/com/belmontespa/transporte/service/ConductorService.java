package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Conductor;
import com.belmontespa.transporte.repository.ConductorRepository;
import org.springframework.stereotype.Service;
@Service
public class ConductorService extends CrudService<Conductor, Long> {
    public ConductorService(ConductorRepository repository) { super(repository); }
}
