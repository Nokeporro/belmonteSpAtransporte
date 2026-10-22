package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Validaciones: formato del 400, normalización y claves de ruta (README, Validaciones y Errores)")
class ValidacionesIT extends IntegrationTest {

	private static final String FOLIO_INEXISTENTE = "TR-9999999";
	private static final String HASH_INEXISTENTE = "0".repeat(32);
	private static final String DNI_HASH_INEXISTENTE = "A".repeat(43);

	@Nested
	@DisplayName("El 400 de validación")
	class Formato400 {

		@Test
		@DisplayName("lleva una línea «campo: motivo» por campo, en orden alfabético y separadas por salto de línea")
		void unaLineaPorCampo() {
			Resp resp = api.post("/moviles", api.admin(), """
					{"patente":"x","capacidad":0,"gpsSerial":-5}""");
			assertThat(resp.status()).isEqualTo(400);
			assertThat(resp.texto("$.message")).isEqualTo("""
					capacidad: Debe ser mayor o igual que 1.
					gpsSerial: Debe ser mayor que 0.
					patente: Tiene un formato inválido.""");
		}

		@Test
		@DisplayName("un campo que falla varias reglas muestra una sola línea, y «Es obligatorio.» gana")
		void unMotivoPorCampo() {
			Resp resp = api.post("/auth/login", null, """
					{"correo":"","password":"x"}""");
			assertThat(resp.status()).isEqualTo(400);
			assertThat(resp.texto("$.message")).isEqualTo("correo: Es obligatorio.");
		}

		@Test
		@DisplayName("los campos anidados se nombran con punto")
		void camposAnidados() {
			Cuenta solicitante = api.solicitante();
			String cuerpo = api.cuerpoViaje(LocalDateTime.now().plusDays(3), "Providencia")
					.replace("-33.426100", "-95.000000");
			Resp resp = api.post("/viajes", solicitante.token(), cuerpo);
			assertThat(resp.status()).isEqualTo(400);
			assertThat(resp.texto("$.message")).isEqualTo("origen.latitud: Debe ser mayor o igual que -90.0.");
		}

		@Test
		@DisplayName("la contraseña indica qué regla incumple")
		void contrasena() {
			String conClave = api.cuerpoSolicitante("pass-" + System.nanoTime() + "@test.cl", "76543210-3", "Empresa");
			assertThat(api.post("/solicitantes", null, conClave.replace(Api.CLAVE, "soloLetras")).texto("$.message"))
					.isEqualTo("password: Debe incluir al menos una letra y un dígito.");
			assertThat(api.post("/solicitantes", null, conClave.replace(Api.CLAVE, "a1")).texto("$.message"))
					.isEqualTo("password: Debe tener entre 8 y 72 caracteres.");
		}
	}

	@Nested
	@DisplayName("Normalización por la API")
	class Normalizacion {

		@Test
		@DisplayName("el conductor se guarda con el correo en minúsculas y el teléfono como +56…")
		void conductor() {
			String correo = "con-norm-" + System.nanoTime() + "@test.cl";
			String cuerpo = """
					{"nombre":" Conductor Norm ","correo":" %s ","telefono":"(9) 3333 3333","password":"%s"}"""
					.formatted(correo.toUpperCase(), Api.CLAVE);
			Resp resp = Api.exigir(api.post("/conductores", api.admin(), cuerpo), "crear conductor", 201);
			assertThat(resp.texto("$.correo")).isEqualTo(correo);
			assertThat(resp.texto("$.nombre")).isEqualTo("Conductor Norm");
			assertThat(resp.texto("$.telefono")).isEqualTo("+56933333333");
		}

		@Test
		@DisplayName("el número de vuelo queda en mayúsculas y sin espacios; un formato inválido responde 400")
		void numeroDeVuelo() {
			Cuenta solicitante = api.solicitante();
			String base = api.cuerpoViaje(LocalDateTime.now().plusDays(3), "Providencia");
			Resp creado = Api.exigir(api.post("/viajes", solicitante.token(), base.replace("LA800", "la 1502")),
					"crear viaje", 201);
			assertThat(creado.texto("$.numeroVuelo")).isEqualTo("LA1502");

			Resp invalido = api.post("/viajes", solicitante.token(), base.replace("LA800", "LA15022"));
			assertThat(invalido.status()).isEqualTo(400);
			assertThat(invalido.texto("$.message")).isEqualTo("numeroVuelo: Tiene un formato inválido.");
		}

		@Test
		@DisplayName("el correo del login no distingue mayúsculas ni espacios en los bordes")
		void loginNormalizaElCorreo() {
			Cuenta solicitante = api.solicitante();
			assertThat(api.iniciarSesion("  " + solicitante.correo().toUpperCase() + " ", Api.CLAVE).status())
					.isEqualTo(200);
		}
	}

	@Nested
	@DisplayName("Claves de ruta mal formadas")
	class ClavesDeRuta {

		@Test
		@DisplayName("un folio, hash o dniHash mal formado en la ruta responde 404, igual que uno inexistente")
		void get() {
			Cuenta ana = api.solicitante();
			String folio = api.viaje(ana);
			assertThat(api.get("/viajes/" + folio, api.admin()).status()).isEqualTo(200);

			assertThat(api.get("/viajes/NOEXISTE", api.admin()).status()).isEqualTo(404);
			assertThat(api.get("/viajes/" + FOLIO_INEXISTENTE, api.admin()).status()).isEqualTo(404);
			assertThat(api.get("/pagos/xyz", api.admin()).status()).isEqualTo(404);
			assertThat(api.get("/pagos/" + HASH_INEXISTENTE, api.admin()).status()).isEqualTo(404);
			assertThat(api.get("/pasajeros/abc", api.admin()).status()).isEqualTo(404);
			assertThat(api.get("/pasajeros/" + DNI_HASH_INEXISTENTE, api.admin()).status()).isEqualTo(404);
			assertThat(api.get("/pasajeros-viaje/NOEXISTE/abc", api.admin()).status()).isEqualTo(404);
			assertThat(api.get("/pasajeros-viaje/%s/%s".formatted(FOLIO_INEXISTENTE, DNI_HASH_INEXISTENTE),
					api.admin()).status()).isEqualTo(404);
		}

		@Test
		@DisplayName("en las acciones, la ruta mal formada (404) se decide antes que el cuerpo inválido (400)")
		void accionesAntesQueElCuerpo() {
			assertThat(api.post("/viajes/NOEXISTE/asignar", api.admin(), "{}").status()).isEqualTo(404);
			assertThat(api.post("/viajes/" + FOLIO_INEXISTENTE + "/asignar", api.admin(), "{}").status())
					.isEqualTo(400);

			Cuenta conductor = api.conductor();
			assertThat(api.post("/viajes/NOEXISTE/estado", conductor.token(), "{}").status()).isEqualTo(404);
			assertThat(api.post("/viajes/" + FOLIO_INEXISTENTE + "/estado", conductor.token(), "{}").status())
					.isEqualTo(400);

			assertThat(api.post("/pagos/xyz/estado", api.admin(), "{}").status()).isEqualTo(404);
			assertThat(api.post("/pagos/" + HASH_INEXISTENTE + "/estado", api.admin(), "{}").status())
					.isEqualTo(400);
		}

		@Test
		@DisplayName("el rol decide antes que el formato: un rol no autorizado recibe 403 aunque la clave esté mal")
		void rolAntesQueFormato() {
			Cuenta solicitante = api.solicitante();
			assertThat(api.get("/pasajeros/abc", solicitante.token()).status()).isEqualTo(403);
			assertThat(api.get("/pagos/xyz", api.conductor().token()).status()).isEqualTo(403);
			assertThat(api.post("/viajes/NOEXISTE/asignar", solicitante.token(), "{}").status()).isEqualTo(403);
		}

		@Test
		@DisplayName("el folio y el dniHash mal formados en el cuerpo responden 400 con el campo")
		void enElCuerpo() {
			Cuenta solicitante = api.solicitante();
			Resp resp = api.post("/pasajeros-viaje", solicitante.token(), """
					{"folio":"VJ123","dniHash":"corto"}""");
			assertThat(resp.status()).isEqualTo(400);
			assertThat(resp.texto("$.message")).isEqualTo("""
					dniHash: Tiene un formato inválido.
					folio: Tiene un formato inválido.""");
		}
	}
}
