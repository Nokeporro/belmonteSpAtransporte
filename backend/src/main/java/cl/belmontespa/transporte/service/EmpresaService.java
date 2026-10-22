package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.response.EmpresaDTOResponse;
import cl.belmontespa.transporte.model.Empresa;
import cl.belmontespa.transporte.repository.EmpresaRepository;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

// Solo lectura por la API: las empresas nacen con el registro del Solicitante (RF01).
@Service
public class EmpresaService extends CrudService<Empresa, String, Void, EmpresaDTOResponse> {

	private final EmpresaRepository empresaRepository;

	public EmpresaService(EmpresaRepository empresaRepository) {
		this.empresaRepository = empresaRepository;
	}

	@Override
	protected JpaRepository<Empresa, Long> repositorio() {
		return empresaRepository;
	}

	@Override
	protected Sort orden() {
		return Sort.by("razonSocial", "rut");
	}

	@Override
	protected Optional<Empresa> buscar(String rut) {
		return empresaRepository.findByRut(rut);
	}

	@Override
	protected EmpresaDTOResponse aResponse(Empresa empresa) {
		return EmpresaDTOResponse.from(empresa);
	}

	// RF01: el registro del Solicitante reutiliza la empresa si el RUT ya existe.
	Empresa obtenerOCrear(String razonSocial, String rut) {
		return empresaRepository.findByRut(rut)
				.orElseGet(() -> empresaRepository.save(nueva(razonSocial, rut)));
	}

	private Empresa nueva(String razonSocial, String rut) {
		Empresa empresa = new Empresa();
		empresa.setRazonSocial(razonSocial);
		empresa.setRut(rut);
		return empresa;
	}
}
