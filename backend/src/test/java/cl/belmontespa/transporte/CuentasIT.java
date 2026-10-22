package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import cl.belmontespa.transporte.validation.Reglas;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("Cuentas: registro, edición, recuperación y acceso a la cuenta propia")
class CuentasIT extends IntegrationTest {

	private static final String TEMPORAL = "Temporal12345";

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// Deja una temporal conocida por SQL, sin pasar por el correo.
	private void dejarTemporal(String correo, String clave, LocalDateTime vence) {
		jdbc.update("UPDATE usuario SET password_temporal_hash = ?, password_temporal_expira = ? WHERE correo = ?",
				passwordEncoder.encode(clave), vence, correo);
	}

	private String temporalGuardada(String correo) {
		return jdbc.queryForObject("SELECT password_temporal_hash FROM usuario WHERE correo = ?", String.class,
				correo);
	}

	// RUT normalizado (sin puntos) y con dígito verificador válido.
	private static String rutUnico() {
		String cuerpo = "75%06d".formatted(System.nanoTime() % 1_000_000);
		return cuerpo + "-" + Reglas.digitoVerificador(cuerpo);
	}

	// "75123456-D" → "75.123.456-D": el formato con que lo escribe una persona.
	private static String conPuntos(String rut) {
		return rut.substring(0, 2) + "." + rut.substring(2, 5) + "." + rut.substring(5);
	}

	private static String correoUnico(String prefijo) {
		return "%s-%d@test.cl".formatted(prefijo, System.nanoTime());
	}

	@Nested
	@DisplayName("Autorregistro del solicitante (RF01)")
	class Registro {

		@Test
		@DisplayName("registra al solicitante y el administrador puede consultarlo")
		void registra() {
			Cuenta cuenta = api.solicitante();
			Resp resp = Api.exigir(api.get("/solicitantes/" + cuenta.correo(), api.admin()), "consultar solicitante",
					200);
			assertThat(resp.texto("$.correo")).isEqualTo(cuenta.correo());
			assertThat(resp.texto("$.razonSocial")).startsWith("Empresa");
		}

		@Test
		@DisplayName("un correo repetido responde 409, aunque cambien las mayúsculas")
		void correoRepetido() {
			Cuenta cuenta = api.solicitante();
			String cuerpo = api.cuerpoSolicitante(cuenta.correo(), rutUnico(), "Otra");
			assertThat(api.post("/solicitantes", null, cuerpo).status()).isEqualTo(409);
			String enMayusculas = api.cuerpoSolicitante(cuenta.correo().toUpperCase(), rutUnico(), "Otra");
			assertThat(api.post("/solicitantes", null, enMayusculas).status()).isEqualTo(409);
		}

		@Test
		@DisplayName("rechaza RUT inválido, contraseña corta y correo mal formado con 400")
		void datosInvalidos() {
			assertThat(api.post("/solicitantes", null, api.cuerpoSolicitante(correoUnico("a"), "12345", "X"))
					.status()).isEqualTo(400);
			assertThat(api.post("/solicitantes", null, api.cuerpoSolicitante("no-es-correo", rutUnico(), "X"))
					.status()).isEqualTo(400);
			String corta = api.cuerpoSolicitante(correoUnico("b"), rutUnico(), "X").replace(Api.CLAVE, "corta");
			assertThat(api.post("/solicitantes", null, corta).status()).isEqualTo(400);
		}

		@Test
		@DisplayName("guarda el correo en minúsculas, el teléfono como +56… y el RUT sin puntos")
		void normaliza() {
			String correo = correoUnico("Norm");
			String rut = rutUnico();
			String cuerpo = """
					{"nombre":"  Ana Pérez  ","correo":"  %s  ","telefono":"9 5555-0101","password":"%s",\
					"razonSocial":" Transportes Norte ","rutEmpresa":"%s"}"""
					.formatted(correo.toUpperCase(), Api.CLAVE, conPuntos(rut));
			Resp resp = Api.exigir(api.post("/solicitantes", null, cuerpo), "registrar con datos sin normalizar", 201);
			assertThat(resp.texto("$.correo")).isEqualTo(correo.toLowerCase());
			assertThat(resp.texto("$.nombre")).isEqualTo("Ana Pérez");
			assertThat(resp.texto("$.telefono")).isEqualTo("+56955550101");
			assertThat(resp.texto("$.razonSocial")).isEqualTo("Transportes Norte");
			assertThat(resp.texto("$.rutEmpresa")).isEqualTo(rut);
			assertThat(api.login(correo.toLowerCase(), Api.CLAVE)).isNotBlank();
		}

		@Test
		@DisplayName("un RUT con dígito verificador incorrecto responde 400")
		void rutConDigitoIncorrecto() {
			String rut = rutUnico();
			char incorrecto = rut.charAt(rut.length() - 1) == '0' ? '1' : '0';
			String conDvMalo = rut.substring(0, rut.length() - 1) + incorrecto;
			Resp resp = api.post("/solicitantes", null, api.cuerpoSolicitante(correoUnico("dv"), conDvMalo, "Empresa"));
			assertThat(resp.status()).isEqualTo(400);
			assertThat(resp.texto("$.message")).isEqualTo("rutEmpresa: Debe ser un RUT válido, como 76543210-3.");
		}

		@Test
		@DisplayName("dos solicitantes con el mismo RUT comparten una sola empresa")
		void empresaReutilizada() {
			String rut = rutUnico();
			api.solicitante(correoUnico("e1"), rut, "Razón Original");
			String segundo = correoUnico("e2");
			Resp registro = Api.exigir(api.post("/solicitantes", null, api.cuerpoSolicitante(segundo, rut, "Nombre Distinto")), "registrar segunda cuenta", 201);
			assertThat(registro.texto("$.estadoSolicitante")).isEqualTo("PENDIENTE");
			assertThat(api.iniciarSesion(segundo, Api.CLAVE).status()).isEqualTo(403);
			Resp empresas = Api.exigir(api.get("/empresas", api.admin()), "listar empresas", 200);
			List<?> coincidencias = empresas.json("$[?(@.rut=='" + rut + "')]");
			assertThat(coincidencias).hasSize(1);
			assertThat(empresas.<List<String>>json("$[?(@.rut=='" + rut + "')].razonSocial")).containsExactly(
					"Razón Original");
		}
	}

	@Nested
	@DisplayName("Alta de conductores por el administrador (CU-07)")
	class AltaDeConductores {

		@Test
		@DisplayName("el conductor nace DISPONIBLE y puede iniciar sesión de inmediato")
		void nace() {
			String correo = correoUnico("con");
			Resp resp = Api.exigir(api.post("/conductores", api.admin(), api.cuerpoConductor(correo)),
					"crear conductor", 201);
			assertThat(resp.texto("$.estadoConductor")).isEqualTo("DISPONIBLE");
			assertThat(api.login(correo, Api.CLAVE)).isNotBlank();
		}

		@Test
		@DisplayName("un correo repetido responde 409")
		void correoRepetido() {
			Cuenta conductor = api.conductor();
			assertThat(api.post("/conductores", api.admin(), api.cuerpoConductor(conductor.correo())).status())
					.isEqualTo(409);
		}

		@Test
		@DisplayName("solo el administrador crea conductores")
		void soloAdministrador() {
			String cuerpo = api.cuerpoConductor(correoUnico("x"));
			assertThat(api.post("/conductores", api.solicitante().token(), cuerpo).status()).isEqualTo(403);
			assertThat(api.post("/conductores", api.conductor().token(), cuerpo).status()).isEqualTo(403);
		}

		@Test
		@DisplayName("el administrador edita datos y contraseña de un conductor; el conductor no puede")
		void edicion() {
			Cuenta conductor = api.conductor();
			String cuerpo = """
					{"nombre":"Conductor Renombrado","correo":"%s","telefono":"+56944444444"}"""
					.formatted(conductor.correo());
			Resp resp = Api.exigir(api.put("/conductores/" + conductor.correo(), api.admin(), cuerpo),
					"editar conductor", 200);
			assertThat(resp.texto("$.nombre")).isEqualTo("Conductor Renombrado");
			assertThat(api.put("/conductores/" + conductor.correo(), conductor.token(), cuerpo).status())
					.isEqualTo(403);
		}
	}

	@Nested
	@DisplayName("Edición de la cuenta propia (RNF08)")
	class Edicion {

		@Test
		@DisplayName("sin enviar contraseña se conserva la actual")
		void sinCambiarClave() {
			Cuenta cuenta = api.solicitante();
			String cuerpo = """
					{"nombre":"Ana Renombrada","correo":"%s","telefono":"+56922222222"}""".formatted(cuenta.correo());
			Resp resp = Api.exigir(api.put("/usuarios/" + cuenta.correo(), cuenta.token(), cuerpo), "editar", 200);
			assertThat(resp.texto("$.nombre")).isEqualTo("Ana Renombrada");
			assertThat(api.iniciarSesion(cuenta.correo(), Api.CLAVE).status()).isEqualTo(200);
		}

		@Test
		@DisplayName("al cambiar la contraseña, la anterior deja de servir")
		void cambiaClave() {
			Cuenta cuenta = api.solicitante();
			String cuerpo = """
					{"nombre":"Ana","correo":"%s","password":"NuevaClave987","passwordActual":"%s"}""".formatted(cuenta.correo(), Api.CLAVE);
			Api.exigir(api.put("/usuarios/" + cuenta.correo(), cuenta.token(), cuerpo), "cambiar clave", 200);
			assertThat(api.iniciarSesion(cuenta.correo(), Api.CLAVE).status()).isEqualTo(401);
			assertThat(api.iniciarSesion(cuenta.correo(), "NuevaClave987").status()).isEqualTo(200);
		}

		@Test
		@DisplayName("no se cambia el correo de la ruta ni se envían datos inválidos (400)")
		void conflictoYValidacion() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			String conCorreoAjeno = """
					{"nombre":"Ana","correo":"%s"}""".formatted(beto.correo());
			assertThat(api.put("/usuarios/" + ana.correo(), ana.token(), conCorreoAjeno).status()).isEqualTo(400);
			assertThat(api.put("/usuarios/" + ana.correo(), ana.token(), "{\"nombre\":\"\",\"correo\":\"x\"}")
					.status()).isEqualTo(400);
		}

		@Test
		@DisplayName("PUT /solicitantes/{correo} no existe: responde 403 a todos, incluido el dueño")
		void noSeEditaPorSolicitantes() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			String cuerpo = api.cuerpoSolicitante(ana.correo(), rutUnico(), "Empresa Renombrada");
			assertThat(api.put("/solicitantes/" + ana.correo(), ana.token(), cuerpo).status()).isEqualTo(403);
			assertThat(api.put("/solicitantes/" + ana.correo(), beto.token(), cuerpo).status()).isEqualTo(403);
			assertThat(api.put("/solicitantes/" + ana.correo(), api.admin(), cuerpo).status()).isEqualTo(403);
		}

		@Test
		@DisplayName("el listado de usuarios devuelve solo la cuenta propia")
		void listadoPropio() {
			Cuenta ana = api.solicitante();
			api.solicitante();
			Resp resp = Api.exigir(api.get("/usuarios", ana.token()), "listar usuarios", 200);
			assertThat(resp.<List<String>>json("$[*].correo")).containsExactly(ana.correo());
		}
	}

	@Nested
	@DisplayName("Recuperar contraseña (CU-13 / RF21)")
	class Recuperar {

		@Test
		@DisplayName("responde 204 exista o no el correo, para no revelar cuentas")
		void respuestaUniforme() {
			Cuenta solicitante = api.solicitante();
			String existente = "{\"correo\":\"%s\"}".formatted(solicitante.correo());
			String fantasma = "{\"correo\":\"%s\"}".formatted(correoUnico("fantasma"));
			assertThat(api.post("/auth/recuperar-password", null, existente).status()).isEqualTo(204);
			assertThat(api.post("/auth/recuperar-password", null, fantasma).status()).isEqualTo(204);
			assertThat(api.post("/auth/recuperar-password", null, "{\"correo\":\"no-es-correo\"}").status())
					.isEqualTo(400);
		}

		@Test
		@DisplayName("guarda una temporal para el solicitante, pero no para un conductor")
		void soloSolicitanteYAdministrador() {
			Cuenta solicitante = api.solicitante();
			Cuenta conductor = api.conductor();
			api.post("/auth/recuperar-password", null, "{\"correo\":\"%s\"}".formatted(solicitante.correo()));
			api.post("/auth/recuperar-password", null, "{\"correo\":\"%s\"}".formatted(conductor.correo()));
			assertThat(temporalGuardada(solicitante.correo())).isNotNull();
			assertThat(temporalGuardada(conductor.correo())).isNull();
		}

		@Test
		@DisplayName("la temporal no reemplaza la contraseña actual: sirven las dos")
		void sirvenLasDos() {
			Cuenta ana = api.solicitante();
			dejarTemporal(ana.correo(), TEMPORAL, LocalDateTime.now().plusMinutes(30));
			assertThat(api.iniciarSesion(ana.correo(), TEMPORAL).status()).isEqualTo(200);
			assertThat(api.iniciarSesion(ana.correo(), Api.CLAVE).status()).isEqualTo(200);
			assertThat(api.iniciarSesion(ana.correo(), TEMPORAL).status()).isEqualTo(401);
		}

		@Test
		@DisplayName("una temporal vencida no sirve, pero la contraseña actual sí")
		void temporalVencida() {
			Cuenta ana = api.solicitante();
			dejarTemporal(ana.correo(), TEMPORAL, LocalDateTime.now().minusMinutes(1));
			assertThat(api.iniciarSesion(ana.correo(), TEMPORAL).status()).isEqualTo(401);
			assertThat(api.iniciarSesion(ana.correo(), Api.CLAVE).status()).isEqualTo(200);
		}

		@Test
		@DisplayName("cambiar la contraseña anula la temporal pendiente")
		void cambiarLaContrasenaAnulaLaTemporal() {
			Cuenta ana = api.solicitante();
			dejarTemporal(ana.correo(), TEMPORAL, LocalDateTime.now().plusMinutes(30));
			String cuerpo = """
					{"nombre":"Ana","correo":"%s","password":"NuevaClave987","passwordActual":"%s"}""".formatted(ana.correo(), Api.CLAVE);
			Api.exigir(api.put("/usuarios/" + ana.correo(), ana.token(), cuerpo), "cambiar clave", 200);
			assertThat(api.iniciarSesion(ana.correo(), TEMPORAL).status()).isEqualTo(401);
			assertThat(temporalGuardada(ana.correo())).isNull();
		}
	}

	@Nested
	@DisplayName("Token FCM único (RF13)")
	class TokenFcm {

		private List<String> duenos(String token) {
			return jdbc.queryForList("SELECT correo FROM usuario WHERE token_fcm = ?", String.class, token);
		}

		@Test
		@DisplayName("un token que usaba otra cuenta pasa a la que inicia sesión después")
		void pasaALaUltimaCuenta() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			String token = "fcm-compartido-" + System.nanoTime();
			Api.exigir(api.iniciarSesion(ana.correo(), Api.CLAVE, token), "login de ana", 200);
			Api.exigir(api.iniciarSesion(beto.correo(), Api.CLAVE, token), "login de beto", 200);
			assertThat(duenos(token)).containsExactly(beto.correo());
		}

		@Test
		@DisplayName("repetir el token de la misma cuenta no falla")
		void mismaCuenta() {
			Cuenta ana = api.solicitante();
			String token = "fcm-propio-" + System.nanoTime();
			Api.exigir(api.iniciarSesion(ana.correo(), Api.CLAVE, token), "primer login", 200);
			Api.exigir(api.iniciarSesion(ana.correo(), Api.CLAVE, token), "segundo login", 200);
			assertThat(duenos(token)).containsExactly(ana.correo());
		}
	}

	@Nested
	@DisplayName("Consulta de cuentas: 403 antes que 404 para no revelar qué correos existen")
	class ConsultaDeCuentas {

		@Test
		@DisplayName("usuarios/{correo}: un correo ajeno responde 403 igual si existe o no")
		void usuarios() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			Resp existente = api.get("/usuarios/" + beto.correo(), ana.token());
			Resp inexistente = api.get("/usuarios/" + correoUnico("fantasma"), ana.token());
			assertThat(existente.status()).isEqualTo(403);
			assertThat(inexistente.status()).isEqualTo(403);
			assertThat(inexistente.texto("$.message")).isEqualTo(existente.texto("$.message"));
			assertThat(api.get("/usuarios/" + correoUnico("fantasma"), api.admin()).status()).isEqualTo(403);
		}

		@Test
		@DisplayName("usuarios/{correo}: la cuenta propia responde 200, también con otras mayúsculas")
		void usuarioPropio() {
			Cuenta ana = api.solicitante();
			assertThat(api.get("/usuarios/" + ana.correo(), ana.token()).status()).isEqualTo(200);
			assertThat(api.get("/usuarios/" + ana.correo().toUpperCase(), ana.token()).status()).isEqualTo(200);
		}

		@Test
		@DisplayName("conductores/{correo}: el conductor ve solo el propio; el administrador ve cualquiera")
		void conductores() {
			Cuenta uno = api.conductor();
			Cuenta otro = api.conductor();
			assertThat(api.get("/conductores/" + uno.correo(), uno.token()).status()).isEqualTo(200);
			Resp existente = api.get("/conductores/" + otro.correo(), uno.token());
			Resp inexistente = api.get("/conductores/" + correoUnico("fantasma"), uno.token());
			assertThat(existente.status()).isEqualTo(403);
			assertThat(inexistente.status()).isEqualTo(403);
			assertThat(inexistente.texto("$.message")).isEqualTo(existente.texto("$.message"));
			assertThat(api.get("/conductores/" + otro.correo(), api.admin()).status()).isEqualTo(200);
			assertThat(api.get("/conductores/" + correoUnico("fantasma"), api.admin()).status()).isEqualTo(404);
		}

		@Test
		@DisplayName("solicitantes/{correo}: el solicitante ve solo el propio; el administrador ve cualquiera")
		void solicitantes() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			assertThat(api.get("/solicitantes/" + ana.correo(), ana.token()).status()).isEqualTo(200);
			Resp existente = api.get("/solicitantes/" + beto.correo(), ana.token());
			Resp inexistente = api.get("/solicitantes/" + correoUnico("fantasma"), ana.token());
			assertThat(existente.status()).isEqualTo(403);
			assertThat(inexistente.status()).isEqualTo(403);
			assertThat(inexistente.texto("$.message")).isEqualTo(existente.texto("$.message"));
			assertThat(api.get("/solicitantes/" + beto.correo(), api.admin()).status()).isEqualTo(200);
			assertThat(api.get("/solicitantes/" + correoUnico("fantasma"), api.admin()).status()).isEqualTo(404);
		}
	}
}
