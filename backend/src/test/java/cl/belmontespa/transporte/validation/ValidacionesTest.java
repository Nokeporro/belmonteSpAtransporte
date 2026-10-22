package cl.belmontespa.transporte.validation;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.dto.request.MovilDTORequest;
import cl.belmontespa.transporte.dto.request.SolicitanteDTORequest;
import cl.belmontespa.transporte.dto.request.UsuarioDTORequest;
import cl.belmontespa.transporte.dto.request.ViajeDTORequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Validaciones: normalización y reglas de los DTO de entrada (README, Validaciones)")
class ValidacionesTest {

	private static final Validator VALIDADOR = Validation.byDefaultProvider().configure()
			.messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();

	/** "campo: motivo" de cada violación, ordenadas. */
	private static List<String> errores(Object dto) {
		return VALIDADOR.validate(dto).stream()
				.map((ConstraintViolation<Object> v) -> v.getPropertyPath() + ": " + v.getMessage())
				.sorted()
				.toList();
	}

	private static UsuarioDTORequest usuario(String nombre, String correo, String telefono, String password) {
		return new UsuarioDTORequest(nombre, correo, telefono, password);
	}

	@Nested
	@DisplayName("Normalizar")
	class Normalizacion {

		@Test
		@DisplayName("el correo se recorta y pasa a minúsculas")
		void correo() {
			assertThat(Normalizar.correo("  Ana.Perez@Test.CL ")).isEqualTo("ana.perez@test.cl");
			assertThat(Normalizar.correo(null)).isNull();
		}

		@Test
		@DisplayName("el teléfono queda como +56 y 9 dígitos, sin espacios, guiones ni paréntesis")
		void telefono() {
			assertThat(Normalizar.telefono("955550101")).isEqualTo("+56955550101");
			assertThat(Normalizar.telefono("9 5555-0101")).isEqualTo("+56955550101");
			assertThat(Normalizar.telefono("+56 9 5555 0101")).isEqualTo("+56955550101");
			assertThat(Normalizar.telefono("(+56) 955550101")).isEqualTo("+56955550101");
			assertThat(Normalizar.telefono("+56955550101")).isEqualTo("+56955550101");
		}

		@Test
		@DisplayName("un teléfono vacío o null es ausente; uno que no es chileno se deja para que falle")
		void telefonoAusenteOInvalido() {
			assertThat(Normalizar.telefono(null)).isNull();
			assertThat(Normalizar.telefono("  ")).isNull();
			assertThat(Normalizar.telefono("12345")).isEqualTo("12345");
		}

		@Test
		@DisplayName("la patente va en mayúsculas, sin espacios ni guion")
		void patente() {
			assertThat(Normalizar.patente(" ab-12 34 ")).isEqualTo("AB1234");
			assertThat(Normalizar.patente("bcdf12")).isEqualTo("BCDF12");
		}

		@Test
		@DisplayName("el RUT queda sin puntos, con guion y K mayúscula")
		void rut() {
			assertThat(Normalizar.rut("76.543.210-3")).isEqualTo("76543210-3");
			assertThat(Normalizar.rut("765432103")).isEqualTo("76543210-3");
			assertThat(Normalizar.rut(" 10.000.013-k ")).isEqualTo("10000013-K");
			assertThat(Normalizar.rut(null)).isNull();
		}

		@Test
		@DisplayName("el número de vuelo va en mayúsculas y sin espacios; vacío es ausente")
		void numeroVuelo() {
			assertThat(Normalizar.numeroVuelo("la 1502")).isEqualTo("LA1502");
			assertThat(Normalizar.numeroVuelo("  ")).isNull();
			assertThat(Normalizar.numeroVuelo(null)).isNull();
		}
	}

	@Nested
	@DisplayName("RUT")
	class DigitoVerificador {

		@Test
		@DisplayName("módulo 11, con los casos 11 → 0 y 10 → K")
		void calcula() {
			assertThat(Reglas.digitoVerificador("76543210")).isEqualTo('3');
			assertThat(Reglas.digitoVerificador("12345678")).isEqualTo('5');
			assertThat(Reglas.digitoVerificador("7654321")).isEqualTo('6');
			assertThat(Reglas.digitoVerificador("10000004")).isEqualTo('0');
			assertThat(Reglas.digitoVerificador("10000013")).isEqualTo('K');
		}

		private List<String> erroresDeRut(String rut) {
			return errores(new SolicitanteDTORequest("Ana", "ana@test.cl", null, "Clave12345", "Empresa", rut));
		}

		@Test
		@DisplayName("acepta RUT de 7 y 8 dígitos con puntos, sin puntos o sin guion")
		void aceptaValidos() {
			assertThat(erroresDeRut("76.543.210-3")).isEmpty();
			assertThat(erroresDeRut("76543210-3")).isEmpty();
			assertThat(erroresDeRut("765432103")).isEmpty();
			assertThat(erroresDeRut("1.234.567-4")).isEmpty();
			assertThat(erroresDeRut("10000004-0")).isEmpty();
			assertThat(erroresDeRut("10000013-k")).isEmpty();
		}

		@Test
		@DisplayName("rechaza un dígito verificador incorrecto, un largo fuera de 7 u 8 dígitos y texto")
		void rechazaInvalidos() {
			String motivo = "rutEmpresa: Debe ser un RUT válido, como 76543210-3.";
			assertThat(erroresDeRut("76543210-4")).containsExactly(motivo);
			assertThat(erroresDeRut("10000013-0")).containsExactly(motivo);
			assertThat(erroresDeRut("123456-5")).containsExactly(motivo);
			assertThat(erroresDeRut("123456789-5")).containsExactly(motivo);
			assertThat(erroresDeRut("abcdefgh-1")).containsExactly(motivo);
		}
	}

	@Nested
	@DisplayName("Contraseña")
	class Contrasenas {

		private List<String> erroresDe(String password) {
			return errores(usuario("Ana", "ana@test.cl", null, password));
		}

		@Test
		@DisplayName("de 8 a 72 caracteres, con al menos una letra y un dígito")
		void acepta() {
			assertThat(erroresDe("Clave1234")).isEmpty();
			assertThat(erroresDe("abcdefg1")).isEmpty();
			assertThat(erroresDe("ñandú2026")).isEmpty();
			assertThat(erroresDe("a1" + "x".repeat(70))).isEmpty();
		}

		@Test
		@DisplayName("null significa conservar la contraseña actual")
		void nullEsValido() {
			assertThat(erroresDe(null)).isEmpty();
		}

		@Test
		@DisplayName("menos de 8 o más de 72 caracteres")
		void largo() {
			String motivo = "password: Debe tener entre 8 y 72 caracteres.";
			assertThat(erroresDe("")).containsExactly(motivo);
			assertThat(erroresDe("Clave12")).containsExactly(motivo);
			assertThat(erroresDe("a1" + "x".repeat(71))).containsExactly(motivo);
		}

		@Test
		@DisplayName("sin letra o sin dígito")
		void composicion() {
			String motivo = "password: Debe incluir al menos una letra y un dígito.";
			assertThat(erroresDe("12345678")).containsExactly(motivo);
			assertThat(erroresDe("abcdefgh")).containsExactly(motivo);
			assertThat(erroresDe("!!!!!!!!!")).containsExactly(motivo);
		}

		@Test
		@DisplayName("hasta 72 caracteres pero más de 72 bytes en UTF-8")
		void bytes() {
			String conTildes = "1" + "á".repeat(37);
			assertThat(conTildes.length()).isLessThanOrEqualTo(72);
			assertThat(erroresDe(conTildes)).containsExactly("password: Debe ocupar como máximo 72 bytes en UTF-8.");
		}
	}

	@Nested
	@DisplayName("Reglas por campo")
	class Campos {

		@Test
		@DisplayName("normaliza antes de validar: un usuario escrito a mano queda válido y guardado normalizado")
		void normalizaAntesDeValidar() {
			UsuarioDTORequest dto = usuario("  Ana Pérez ", " ANA@Test.cl ", "9 5555-0101", "Clave12345");
			assertThat(errores(dto)).isEmpty();
			assertThat(dto.nombre()).isEqualTo("Ana Pérez");
			assertThat(dto.correo()).isEqualTo("ana@test.cl");
			assertThat(dto.telefono()).isEqualTo("+56955550101");
		}

		@Test
		@DisplayName("nombre de 2 a 120 caracteres; el motivo lleva mayúscula inicial, punto final y no el valor")
		void nombre() {
			assertThat(errores(usuario("A", "ana@test.cl", null, null)))
					.containsExactly("nombre: Debe tener entre 2 y 120 caracteres.");
			assertThat(errores(usuario("   ", "ana@test.cl", null, null)))
					.contains("nombre: Es obligatorio.");
			assertThat(errores(usuario("x".repeat(121), "ana@test.cl", null, null)))
					.containsExactly("nombre: Debe tener entre 2 y 120 caracteres.");
			assertThat(errores(usuario("Al", "ana@test.cl", null, null))).isEmpty();
		}

		@Test
		@DisplayName("razonSocial de 2 a 50 caracteres")
		void razonSocial() {
			assertThat(errores(new SolicitanteDTORequest("Ana", "ana@test.cl", null, null, "E", "76543210-3")))
					.containsExactly("razonSocial: Debe tener entre 2 y 50 caracteres.");
			assertThat(errores(new SolicitanteDTORequest("Ana", "ana@test.cl", null, null, "x".repeat(51),
					"76543210-3"))).containsExactly("razonSocial: Debe tener entre 2 y 50 caracteres.");
		}

		@Test
		@DisplayName("correo con arroba y dominio con punto, hasta 150 caracteres")
		void correo() {
			assertThat(errores(usuario("Ana", "ana@test", null, null)))
					.containsExactly("correo: Tiene un formato inválido.");
			assertThat(errores(usuario("Ana", "ana test@test.cl", null, null)))
					.containsExactly("correo: Tiene un formato inválido.");
			assertThat(errores(usuario("Ana", "x".repeat(145) + "@a.cl", null, null))).isEmpty();
			assertThat(errores(usuario("Ana", "x".repeat(146) + "@a.cl", null, null)))
					.containsExactly("correo: Debe tener como máximo 150 caracteres.");
			assertThat(errores(usuario("Ana", "", null, null))).contains("correo: Es obligatorio.");
		}

		@Test
		@DisplayName("teléfono chileno de 9 dígitos")
		void telefono() {
			assertThat(errores(usuario("Ana", "ana@test.cl", "12345", null)))
					.containsExactly("telefono: Tiene un formato inválido.");
			assertThat(errores(usuario("Ana", "ana@test.cl", "+5695555010", null)))
					.containsExactly("telefono: Tiene un formato inválido.");
			assertThat(errores(usuario("Ana", "ana@test.cl", null, null))).isEmpty();
		}

		@Test
		@DisplayName("patente AA1234 o BBBB12; capacidad de 1 a 20; gpsSerial mayor que 0")
		void movil() {
			assertThat(errores(new MovilDTORequest("ab-1234", 20, 1))).isEmpty();
			assertThat(errores(new MovilDTORequest("bcdf12", 1, null))).isEmpty();
			assertThat(errores(new MovilDTORequest("AB12CD", 2, null)))
					.containsExactly("patente: Tiene un formato inválido.");
			assertThat(errores(new MovilDTORequest("ABC123", 2, null)))
					.containsExactly("patente: Tiene un formato inválido.");
			assertThat(errores(new MovilDTORequest("AB1234", 0, null)))
					.containsExactly("capacidad: Debe ser mayor o igual que 1.");
			assertThat(errores(new MovilDTORequest("AB1234", 21, null)))
					.containsExactly("capacidad: Debe ser menor o igual que 20.");
			assertThat(errores(new MovilDTORequest("AB1234", 2, 0)))
					.containsExactly("gpsSerial: Debe ser mayor que 0.");
			assertThat(errores(new MovilDTORequest("AB1234", null, null)))
					.containsExactly("capacidad: Es obligatorio.");
		}

		@Test
		@DisplayName("número de vuelo: 2 caracteres, 1 a 4 dígitos y una letra opcional")
		void numeroVuelo() {
			assertThat(viaje(" la 1502 ").numeroVuelo()).isEqualTo("LA1502");
			assertThat(errores(viaje("la 1502"))).isEmpty();
			assertThat(errores(viaje("LA1502B"))).isEmpty();
			assertThat(errores(viaje("L12"))).isEmpty();
			assertThat(errores(viaje(null))).isEmpty();
			assertThat(errores(viaje("  "))).isEmpty();
			assertThat(errores(viaje("LA15022"))).containsExactly("numeroVuelo: Tiene un formato inválido.");
			assertThat(errores(viaje("LA"))).containsExactly("numeroVuelo: Tiene un formato inválido.");
			assertThat(errores(viaje("L-1502"))).containsExactly("numeroVuelo: Tiene un formato inválido.");
		}

		@Test
		@DisplayName("los campos anidados se nombran con punto: origen.latitud")
		void anidados() {
			var origen = new ViajeDTORequest.Ubicacion("  Av. Providencia 1234 ", "Providencia", "Metropolitana",
					new BigDecimal("-95.0"), new BigDecimal("-70.6"));
			var destino = new ViajeDTORequest.Ubicacion("Av. Apoquindo 4500", "Las Condes", "Metropolitana",
					new BigDecimal("-33.4"), new BigDecimal("-70.5"));
			var dto = new ViajeDTORequest(LocalDateTime.now().plusDays(3), origen, destino, null);
			assertThat(errores(dto)).containsExactly("origen.latitud: Debe ser mayor o igual que -90.0.");
			assertThat(dto.origen().direccion()).isEqualTo("Av. Providencia 1234");
		}

		private ViajeDTORequest viaje(String numeroVuelo) {
			var ubicacion = new ViajeDTORequest.Ubicacion("Av. Providencia 1234", "Providencia", "Metropolitana",
					new BigDecimal("-33.4261"), new BigDecimal("-70.611"));
			return new ViajeDTORequest(LocalDateTime.now().plusDays(3), ubicacion, ubicacion, numeroVuelo);
		}
	}

	@Nested
	@DisplayName("Claves de ruta")
	class Claves {

		@Test
		@DisplayName("folio TR- y un correlativo de 4 a 17 dígitos que no empieza en 0")
		void folio() {
			assertThat(Reglas.esFolio("TR-1001")).isTrue();
			assertThat(Reglas.esFolio("TR-99999999999999999")).isTrue();
			assertThat(Reglas.esFolio("TR-999")).isFalse();
			assertThat(Reglas.esFolio("TR-0123")).isFalse();
			assertThat(Reglas.esFolio("TR-100000000000000000")).isFalse();
			assertThat(Reglas.esFolio("VJ1001")).isFalse();
			assertThat(Reglas.esFolio("tr-1001")).isFalse();
			assertThat(Reglas.esFolio("TR-1001\n")).isFalse();
			assertThat(Reglas.esFolio(null)).isFalse();
		}

		@Test
		@DisplayName("hash de pago: 32 hexadecimales en minúsculas")
		void hashPago() {
			assertThat(Reglas.esHashPago("0123456789abcdef0123456789abcdef")).isTrue();
			assertThat(Reglas.esHashPago("0123456789ABCDEF0123456789ABCDEF")).isFalse();
			assertThat(Reglas.esHashPago("0123456789abcdef")).isFalse();
			assertThat(Reglas.esHashPago("g123456789abcdef0123456789abcdef")).isFalse();
			assertThat(Reglas.esHashPago(null)).isFalse();
		}

		@Test
		@DisplayName("dniHash: 43 caracteres Base64url")
		void dniHash() {
			assertThat(Reglas.esDniHash("A".repeat(43))).isTrue();
			assertThat(Reglas.esDniHash("a-_".repeat(14) + "a")).isTrue();
			assertThat(Reglas.esDniHash("A".repeat(42))).isFalse();
			assertThat(Reglas.esDniHash("A".repeat(44))).isFalse();
			assertThat(Reglas.esDniHash("A".repeat(42) + "=")).isFalse();
			assertThat(Reglas.esDniHash("A".repeat(42) + "+")).isFalse();
			assertThat(Reglas.esDniHash(null)).isFalse();
		}
	}
}
