package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
