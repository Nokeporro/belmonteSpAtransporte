package com.belmontespa.transporte.repository;
import com.belmontespa.transporte.model.DetalleViajePasajero;
import com.belmontespa.transporte.model.DetalleViajePasajeroId;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DetalleViajePasajeroRepository extends JpaRepository<DetalleViajePasajero, DetalleViajePasajeroId> {}
