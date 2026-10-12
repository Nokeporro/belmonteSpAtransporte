package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Movil;
import com.belmontespa.transporte.repository.MovilRepository;
import org.springframework.stereotype.Service;
@Service
public class MovilService extends CrudService<Movil, Long> {
    public MovilService(MovilRepository repository) { super(repository); }
}
