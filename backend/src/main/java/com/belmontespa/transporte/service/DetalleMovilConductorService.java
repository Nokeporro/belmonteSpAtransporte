package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.DetalleMovilConductor;
import com.belmontespa.transporte.model.DetalleMovilConductorId;
import com.belmontespa.transporte.repository.DetalleMovilConductorRepository;
import org.springframework.stereotype.Service;
@Service
public class DetalleMovilConductorService extends CrudService<DetalleMovilConductor, DetalleMovilConductorId> {
    public DetalleMovilConductorService(DetalleMovilConductorRepository repository) { super(repository); }
}
