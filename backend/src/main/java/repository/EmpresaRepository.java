package com.belmontespa.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
