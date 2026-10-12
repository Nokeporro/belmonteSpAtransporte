package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Solicitante;
import com.belmontespa.transporte.repository.SolicitanteRepository;
import org.springframework.stereotype.Service;
@Service
public class SolicitanteService extends CrudService<Solicitante, Long> {
    public SolicitanteService(SolicitanteRepository repository) { super(repository); }
}
