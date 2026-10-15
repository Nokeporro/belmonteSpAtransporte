package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.TipoRecibo;
import com.belmontespa.transporte.repository.TipoReciboRepository;
import org.springframework.stereotype.Service;
@Service
public class TipoReciboService extends CrudService<TipoRecibo, Long> {
    public TipoReciboService(TipoReciboRepository repository) { super(repository); }
}
