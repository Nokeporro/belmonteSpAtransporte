package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Banco;
import com.belmontespa.transporte.repository.BancoRepository;
import org.springframework.stereotype.Service;
@Service
public class BancoService extends CrudService<Banco, Long> {
    public BancoService(BancoRepository repository) { super(repository); }
}
