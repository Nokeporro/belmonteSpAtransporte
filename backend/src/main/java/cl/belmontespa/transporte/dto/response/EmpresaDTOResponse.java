package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Empresa;

public record EmpresaDTOResponse(
		String razonSocial,
		String rut
) {

	public static EmpresaDTOResponse from(Empresa empresa) {
		return new EmpresaDTOResponse(empresa.getRazonSocial(), empresa.getRut());
	}
}
