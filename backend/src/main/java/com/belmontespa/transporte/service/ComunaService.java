package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Comuna;
import com.belmontespa.transporte.repository.ComunaRepository;
import org.springframework.stereotype.Service;
@Service
public class ComunaService extends CrudService<Comuna, Long> {
    public ComunaService(ComunaRepository repository) { super(repository); }
}
