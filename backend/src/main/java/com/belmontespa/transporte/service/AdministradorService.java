package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Administrador;
import com.belmontespa.transporte.repository.AdministradorRepository;
import org.springframework.stereotype.Service;
@Service
public class AdministradorService extends CrudService<Administrador, Long> {
    public AdministradorService(AdministradorRepository repository) { super(repository); }
}
