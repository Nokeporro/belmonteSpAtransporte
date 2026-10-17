package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.EmpresaDTORequest;
import cl.belmontespa.transporte.dto.response.EmpresaDTOResponse;
import cl.belmontespa.transporte.model.Empresa;
import cl.belmontespa.transporte.repository.EmpresaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmpresaService extends CrudService<Empresa, String, EmpresaDTORequest, EmpresaDTOResponse> {

	private final EmpresaRepository empresaRepository;

	public EmpresaService(EmpresaRepository empresaRepository) {
		this.empresaRepository = empresaRepository;
	}

	@Override
	protected JpaRepository<Empresa, Long> repositorio() {
		return empresaRepository;
	}

	@Override
	protected Optional<Empresa> buscar(String rut) {
		return empresaRepository.findByRut(rut);
	}

	@Override
	protected Empresa nuevaEntidad(EmpresaDTORequest request) {
		if (empresaRepository.findByRut(request.rut()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El RUT ya está registrado");
		}
		return nueva(request.razonSocial(), request.rut());
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
