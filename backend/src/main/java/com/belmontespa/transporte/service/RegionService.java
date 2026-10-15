package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Region;
import com.belmontespa.transporte.repository.RegionRepository;
import org.springframework.stereotype.Service;
@Service
public class RegionService extends CrudService<Region, Long> {
    public RegionService(RegionRepository repository) { super(repository); }
}
