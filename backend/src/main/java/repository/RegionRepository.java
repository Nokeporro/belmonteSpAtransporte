package com.belmontespa.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.belmontespa.transporte.model.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
}
