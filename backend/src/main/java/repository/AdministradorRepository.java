package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Administrador;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
