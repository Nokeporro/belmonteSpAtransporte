package com.belmontespa.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.Comuna;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {
}
