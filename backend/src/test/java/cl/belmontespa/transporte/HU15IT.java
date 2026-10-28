package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class HU15IT extends IntegrationTest {

	@Autowired JdbcTemplate jdbc;
	@Autowired PasswordEncoder encoder;

	@Test
	void normalizaYEntregaUnaCuentaActivaConCredencialesDefinitivas() {
		String correo = "alta-" + System.nanoTime() + "@test.cl";
		Resp alta = Api.exigir(api.post("/conductores", api.admin(), """
				{"nombre":"  Ana Conductora  ","correo":"  %s  ","telefono":"9 5555-0101","password":"Definitiva123"}
				""".formatted(correo.toUpperCase())), "alta normalizada", 201);
		assertThat(alta.texto("$.nombre")).isEqualTo("Ana Conductora");
		assertThat(alta.texto("$.correo")).isEqualTo(correo);
		assertThat(alta.texto("$.telefono")).isEqualTo("+56955550101");
		assertThat(alta.<Boolean>json("$.activo")).isTrue();
		assertThat(alta.texto("$.estadoConductor")).isEqualTo("DISPONIBLE");
		assertThat(alta.entero("$.viajesAsignados")).isZero();
		assertThat(alta.texto("$.folioEnCurso")).isNull();
		assertThat(alta.cuerpo()).doesNotContain("password", "idUsuario");
		assertThat(Api.exigir(api.iniciarSesion(correo, "Definitiva123"), "login inmediato", 200)
				.<Boolean>json("$.cambioPasswordRequerido")).isFalse();
		String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE correo = ?", String.class, correo);
		assertThat(hash).isNotEqualTo("Definitiva123");
		assertThat(encoder.matches("Definitiva123", hash)).isTrue();
	}

	@Test
	void elCorreoEsUnicoEntreTodosLosRolesSinReemplazarLaCuenta() {
		var existente = api.solicitante();
		Resp duplicado = api.post("/conductores", api.admin(), api.cuerpoConductor(existente.correo().toUpperCase()));
		assertThat(duplicado.status()).isEqualTo(409);
		assertThat(api.iniciarSesion(existente.correo(), Api.CLAVE).status()).isEqualTo(200);
		assertThat(api.get("/conductores/" + existente.correo(), api.admin()).status()).isEqualTo(404);
	}

	@Test
	void altaSinPasswordOConDatosInvalidosNoCreaUsuario() {
		String correo = "invalido-" + System.nanoTime() + "@test.cl";
		String sinPassword = "{\"nombre\":\"Conductor\",\"correo\":\"%s\"}".formatted(correo);
		assertThat(api.post("/conductores", api.admin(), sinPassword).status()).isEqualTo(400);
		for (String password : List.of("corta1", "abcdefgh", "12345678", "é".repeat(36) + "1")) {
			assertThat(api.post("/conductores", api.admin(), api.cuerpoConductor(correo).replace(Api.CLAVE, password)).status()).isEqualTo(400);
		}
		assertThat(api.post("/conductores", api.admin(), api.cuerpoConductor(correo).replace("+56933333333", "123")).status()).isEqualTo(400);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo = ?", Long.class, correo)).isZero();
	}

	@Test
	void noPermiteCambiarElCorreoNiAplicaCambiosParciales() {
		var conductor = api.conductor();
		String otro = "otro-" + System.nanoTime() + "@test.cl";
		Resp cambio = api.put("/conductores/" + conductor.correo(), api.admin(), """
				{"nombre":"No se guarda","correo":"%s","password":"NuevaClave123"}
				""".formatted(otro));
		assertThat(cambio.status()).isEqualTo(400);
		assertThat(cambio.texto("$.message")).isEqualTo("correo: Debe coincidir con el de la ruta.");
		assertThat(api.get("/conductores/" + conductor.correo(), api.admin()).texto("$.nombre")).isEqualTo("Conductor");
		assertThat(api.iniciarSesion(conductor.correo(), Api.CLAVE).status()).isEqualTo(200);
		assertThat(api.get("/conductores/" + otro, api.admin()).status()).isEqualTo(404);
	}

	@Test
	void editaSinPasswordYLaReemplazaSinPedirLaActual() {
		var conductor = api.conductor();
		String ruta = "/conductores/" + conductor.correo();
		Resp editado = Api.exigir(api.put(ruta, api.admin(), """
				{"nombre":"  Nombre Editado  ","correo":"%s","telefono":"9 4444-2222"}
				""".formatted(conductor.correo().toUpperCase())), "editar sin contraseña", 200);
		assertThat(editado.texto("$.correo")).isEqualTo(conductor.correo());
		assertThat(editado.texto("$.telefono")).isEqualTo("+56944442222");
		assertThat(api.iniciarSesion(conductor.correo(), Api.CLAVE).status()).isEqualTo(200);
		jdbc.update("UPDATE usuario SET password_temporal_hash = ?, password_temporal_expira = ? WHERE correo = ?",
				encoder.encode("Temporal123"), LocalDateTime.now(ClockConfig.ZONA).plusHours(1), conductor.correo());
		Api.exigir(api.put(ruta, api.admin(), """
				{"nombre":"Nombre Editado","correo":"%s","password":"NuevaClave123"}
				""".formatted(conductor.correo())), "reemplazar contraseña", 200);
		assertThat(api.iniciarSesion(conductor.correo(), Api.CLAVE).status()).isEqualTo(401);
		assertThat(api.iniciarSesion(conductor.correo(), "Temporal123").status()).isEqualTo(401);
		assertThat(api.iniciarSesion(conductor.correo(), "NuevaClave123").status()).isEqualTo(200);
		assertThat(jdbc.queryForObject("SELECT password_temporal_hash FROM usuario WHERE correo = ?", String.class, conductor.correo())).isNull();
	}

	@Test
	void resumenDeAsignacionesSeActualizaAlIniciarYFinalizar() {
		var solicitante = api.solicitante();
		var flota = api.flota(4);
		var base = LocalDateTime.now(ClockConfig.ZONA).plusDays(3);
		String primero = api.viajeAsignado(solicitante, flota, base);
		String segundo = api.viajeAsignado(solicitante, flota, base.plusDays(1));
		var ajena = api.flota(4);
		api.viajeAsignado(solicitante, ajena, base);
		String ruta = "/conductores/" + flota.conductor().correo();
		assertThat(api.get(ruta, api.admin()).entero("$.viajesAsignados")).isEqualTo(2);
		jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", LocalDateTime.now(ClockConfig.ZONA), primero);
		api.cambiarEstadoOk(primero, flota.conductor(), "EN_CURSO");
		Resp ocupado = Api.exigir(api.get(ruta, flota.conductor().token()), "consulta propia", 200);
		assertThat(ocupado.entero("$.viajesAsignados")).isEqualTo(1);
		assertThat(ocupado.texto("$.folioEnCurso")).isEqualTo(primero);
		Resp lista = Api.exigir(api.get("/conductores", api.admin()), "listar con resumen", 200);
		String fila = "$[?(@.correo=='" + flota.conductor().correo() + "')]";
		assertThat(lista.<List<Integer>>json(fila + ".viajesAsignados")).containsExactly(1);
		assertThat(lista.<List<String>>json(fila + ".folioEnCurso")).containsExactly(primero);
		api.cambiarEstadoOk(primero, flota.conductor(), "FINALIZADO");
		assertThat(api.get(ruta, api.admin()).texto("$.folioEnCurso")).isNull();
		assertThat(api.get(ruta, api.admin()).entero("$.viajesAsignados")).isEqualTo(1);
		assertThat(api.estadoViaje(segundo)).isEqualTo("ASIGNADO");
	}

	@Test
	void consultaYEdicionRespetanRolesYCuentaPropia() {
		var conductor = api.conductor();
		var otro = api.conductor();
		String ruta = "/conductores/" + conductor.correo();
		assertThat(api.get(ruta, otro.token()).status()).isEqualTo(403);
		assertThat(api.get(ruta, api.solicitante().token()).status()).isEqualTo(403);
		assertThat(api.get("/conductores", conductor.token()).status()).isEqualTo(403);
		assertThat(api.put(ruta, conductor.token(), api.cuerpoConductor(conductor.correo())).status()).isEqualTo(403);
	}
}
