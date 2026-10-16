package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Flota: móviles (CU-09) y habilitaciones conductor-móvil (CU-10)")
class FlotaIT extends IntegrationTest {

	@Test
	@DisplayName("el móvil nace DISPONIBLE con su capacidad")
	void movilNace() {
		String patente = api.movil(3);
		Resp resp = Api.exigir(api.get("/moviles/" + patente, api.admin()), "consultar móvil", 200);
		assertThat(resp.texto("$.estadoMovil")).isEqualTo("DISPONIBLE");
		assertThat(resp.entero("$.capacidad")).isEqualTo(3);
	}

	@Test
	@DisplayName("una patente repetida responde 409")
	void patenteRepetida() {
		String patente = api.movil(2);
		assertThat(api.post("/moviles", api.admin(), api.cuerpoMovil(patente, 2)).status()).isEqualTo(409);
	}

	@Test
	@DisplayName("rechaza patente en minúsculas, patente corta y capacidad no positiva con 400")
	void datosInvalidos() {
		assertThat(api.post("/moviles", api.admin(), api.cuerpoMovil("ab1234", 2)).status()).isEqualTo(400);
		assertThat(api.post("/moviles", api.admin(), api.cuerpoMovil("AB12", 2)).status()).isEqualTo(400);
		assertThat(api.post("/moviles", api.admin(), api.cuerpoMovil("ZZ9999", 0)).status()).isEqualTo(400);
		assertThat(api.post("/moviles", api.admin(), api.cuerpoMovil("ZZ9999", -1)).status()).isEqualTo(400);
	}

	@Test
	@DisplayName("solo el administrador gestiona móviles")
	void soloAdministrador() {
		Cuenta conductor = api.conductor();
		Cuenta solicitante = api.solicitante();
		assertThat(api.post("/moviles", conductor.token(), api.cuerpoMovil("ZZ9998", 2)).status()).isEqualTo(403);
		assertThat(api.post("/moviles", solicitante.token(), api.cuerpoMovil("ZZ9998", 2)).status()).isEqualTo(403);
		assertThat(api.get("/moviles", conductor.token()).status()).isEqualTo(403);
	}

	@Test
	@DisplayName("un móvil inexistente responde 404")
	void movilInexistente() {
		assertThat(api.get("/moviles/NOEXIS", api.admin()).status()).isEqualTo(404);
	}

	@Test
	@DisplayName("habilitar es idempotente: repetir no duplica la relación")
	void habilitarEsIdempotente() {
		Cuenta conductor = api.conductor();
		String patente = api.movil(2);
		api.habilitarOk(patente, conductor.correo());
		api.habilitarOk(patente, conductor.correo());
		Resp habilitaciones = Api.exigir(api.get("/habilitaciones-movil-conductor", api.admin()), "listar", 200);
		assertThat(habilitaciones.<java.util.List<?>>json(
				"$[?(@.patente=='%s' && @.correoConductor=='%s')]".formatted(patente, conductor.correo())))
				.hasSize(1);
		assertThat(api.get("/habilitaciones-movil-conductor/%s/%s".formatted(patente, conductor.correo()),
				api.admin()).status()).isEqualTo(200);
	}

	@Test
	@DisplayName("habilitar con un móvil o conductor inexistente responde 404")
	void habilitarInexistentes() {
		Cuenta conductor = api.conductor();
		String patente = api.movil(2);
		assertThat(api.habilitar("ZZ0000", conductor.correo()).status()).isEqualTo(404);
		assertThat(api.habilitar(patente, "nadie-" + System.nanoTime() + "@test.cl").status()).isEqualTo(404);
	}

	@Test
	@DisplayName("solo el administrador habilita conductores")
	void habilitarSoloAdministrador() {
		Cuenta conductor = api.conductor();
		String patente = api.movil(2);
		String cuerpo = """
				{"patente":"%s","correoConductor":"%s"}""".formatted(patente, conductor.correo());
		assertThat(api.post("/habilitaciones-movil-conductor", conductor.token(), cuerpo).status()).isEqualTo(403);
		assertThat(api.post("/habilitaciones-movil-conductor", api.solicitante().token(), cuerpo).status())
				.isEqualTo(403);
	}
}
