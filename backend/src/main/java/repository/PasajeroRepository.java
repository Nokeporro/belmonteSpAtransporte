package com.belmontespa.transporte.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.Pasajero;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long> {
    Optional<Pasajero> findByRutHash(String rutHash);
}
