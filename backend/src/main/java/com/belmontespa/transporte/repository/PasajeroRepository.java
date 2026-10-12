package com.belmontespa.transporte.repository;
import com.belmontespa.transporte.model.Pasajero;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PasajeroRepository extends JpaRepository<Pasajero, Long> {}
