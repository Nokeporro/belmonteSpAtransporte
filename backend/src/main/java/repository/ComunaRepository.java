package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Comuna;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {
}
