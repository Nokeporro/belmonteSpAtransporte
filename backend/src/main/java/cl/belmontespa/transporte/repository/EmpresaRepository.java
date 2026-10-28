package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Empresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

	Optional<Empresa> findByRut(String rut);
}
