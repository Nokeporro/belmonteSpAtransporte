package com.belmontespa.transporte.repository;
import com.belmontespa.transporte.model.DetalleMovilConductor;
import com.belmontespa.transporte.model.DetalleMovilConductorId;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DetalleMovilConductorRepository extends JpaRepository<DetalleMovilConductor, DetalleMovilConductorId> {}
