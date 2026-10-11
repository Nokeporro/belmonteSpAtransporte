package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
}
