package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.EstadoSolicitud;
import com.belmontespa.transporte.repository.EstadoSolicitudRepository;
import org.springframework.stereotype.Service;
@Service
public class EstadoSolicitudService extends CrudService<EstadoSolicitud, Long> {
    public EstadoSolicitudService(EstadoSolicitudRepository repository) { super(repository); }
}
