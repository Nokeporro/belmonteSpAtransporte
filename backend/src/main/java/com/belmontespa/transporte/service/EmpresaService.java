package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Empresa;
import com.belmontespa.transporte.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
@Service
public class EmpresaService extends CrudService<Empresa, Long> {
    public EmpresaService(EmpresaRepository repository) { super(repository); }
}
