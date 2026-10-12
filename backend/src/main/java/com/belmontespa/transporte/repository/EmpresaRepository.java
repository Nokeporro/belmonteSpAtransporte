package com.belmontespa.transporte.repository;
import com.belmontespa.transporte.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {}
