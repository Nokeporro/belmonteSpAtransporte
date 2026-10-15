package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.EstadoMovil;
import com.belmontespa.transporte.repository.EstadoMovilRepository;
import org.springframework.stereotype.Service;
@Service
public class EstadoMovilService extends CrudService<EstadoMovil, Long> {
    public EstadoMovilService(EstadoMovilRepository repository) { super(repository); }
}
