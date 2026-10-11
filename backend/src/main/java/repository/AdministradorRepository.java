package com.belmontespa.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.Administrador;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
