package cl.belmontespa.transporte.dto.request;

import cl.belmontespa.transporte.validation.Contrasena;
import cl.belmontespa.transporte.validation.Normalizar;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdministradorDTORequest(
		@NotBlank @Size(min = 2, max = 120) String nombre,
		@NotBlank @Pattern(regexp = Reglas.CORREO) @Size(max = 150, message = Reglas.MAXIMO) String correo,
		@Pattern(regexp = Reglas.TELEFONO) String telefono,
		@NotBlank @Contrasena String password
) {

	public AdministradorDTORequest {
		nombre = Normalizar.texto(nombre);
		correo = Normalizar.correo(correo);
		telefono = Normalizar.telefono(telefono);
	}
}
