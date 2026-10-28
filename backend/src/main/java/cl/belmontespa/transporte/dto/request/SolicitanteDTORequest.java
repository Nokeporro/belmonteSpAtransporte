package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Contrasena;
import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import cl.belmontespa.transporte.validation.Rut;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitanteDTORequest(
		@NotBlank @Size(min = 2, max = 120) String nombre,
		@NotBlank @Pattern(regexp = Reglas.CORREO) @Size(max = 150, message = Reglas.MAXIMO) String correo,
		@Pattern(regexp = Reglas.TELEFONO) String telefono,
		@Contrasena String password,
		@NotBlank @Size(min = 2, max = 50) String razonSocial,
		@NotBlank @Rut String rutEmpresa
) {

	public SolicitanteDTORequest {
		nombre = Normalizar.texto(nombre);
		correo = Normalizar.correo(correo);
		telefono = Normalizar.telefono(telefono);
		razonSocial = Normalizar.texto(razonSocial);
		rutEmpresa = Normalizar.rut(rutEmpresa);
	}
}
