package cl.belmontespa.transporte.repository;

import cl.belmontespa.transporte.model.Empresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

	// RF01: el registro del Solicitante crea o reutiliza la empresa por RUT.
	Optional<Empresa> findByRut(String rut);
}
