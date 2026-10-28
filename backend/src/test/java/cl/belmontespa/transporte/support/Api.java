package cl.belmontespa.transporte.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.validation.Reglas;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class Api {

	public static final String CLAVE = "Clave12345";
	public static final String ADMIN_CORREO = "admin@belmontespa.cl";
	public static final String ADMIN_CLAVE = "Belmonte2026!";

	private static final AtomicInteger CONTADOR = new AtomicInteger();
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final MockMvc mvc;
	private final JdbcTemplate jdbc;
	private String tokenAdmin;

	public Api(MockMvc mvc, JdbcTemplate jdbc) {
		this.mvc = mvc;
		this.jdbc = jdbc;
	}

	public record Resp(int status, String cuerpo) {

		public <T> T json(String path) {
			return JsonPath.read(cuerpo, path);
		}

		public String texto(String path) {
			Object valor = json(path);
			return valor == null ? null : String.valueOf(valor);
		}

		public int entero(String path) {
			return ((Number) json(path)).intValue();
		}
	}

	public record Cuenta(String correo, String token) {
	}


	public Resp enviar(HttpMethod metodo, String ruta, String token, String cuerpo) {
		MockHttpServletRequestBuilder peticion = request(metodo, "/api/v1" + ruta);
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		if (cuerpo != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		try {
			var respuesta = mvc.perform(peticion).andReturn().getResponse();
			return new Resp(respuesta.getStatus(), respuesta.getContentAsString(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IllegalStateException("Falló la petición " + metodo + " " + ruta, e);
		}
	}

	public Resp get(String ruta, String token) {
		return enviar(HttpMethod.GET, ruta, token, null);
	}

	public Resp post(String ruta, String token, String cuerpo) {
		return enviar(HttpMethod.POST, ruta, token, cuerpo);
	}

	public Resp put(String ruta, String token, String cuerpo) {
		return enviar(HttpMethod.PUT, ruta, token, cuerpo);
	}

	public static Resp exigir(Resp resp, String que, int... esperados) {
		assertThat(Arrays.stream(esperados).anyMatch(e -> e == resp.status()))
				.as("%s: esperado %s, obtenido %d, cuerpo: %s", que, Arrays.toString(esperados), resp.status(),
						resp.cuerpo())
				.isTrue();
		return resp;
	}


	public Resp iniciarSesion(String correo, String clave) {
		return post("/auth/login", null, """
				{"correo":"%s","password":"%s"}""".formatted(correo, clave));
	}

	public Resp iniciarSesion(String correo, String clave, String tokenFcm) {
		return post("/auth/login", null, """
				{"correo":"%s","password":"%s","tokenFcm":"%s"}""".formatted(correo, clave, tokenFcm));
	}

	public Resp recuperarPassword(String token, String correo) {
		return post("/auth/recuperar-password", token, """
				{"correo":"%s"}""".formatted(correo));
	}

	public String dispositivo(Cuenta cuenta) {
		String tokenFcm = "fcm-" + cuenta.correo();
		exigir(iniciarSesion(cuenta.correo(), CLAVE, tokenFcm), "registrar dispositivo de " + cuenta.correo(), 200);
		return tokenFcm;
	}

	public String dispositivoDelAdministrador() {
		admin();
		String tokenFcm = "fcm-" + ADMIN_CORREO;
		exigir(iniciarSesion(ADMIN_CORREO, ADMIN_CLAVE, tokenFcm), "registrar dispositivo del administrador", 200);
		return tokenFcm;
	}

	public String login(String correo, String clave) {
		return exigir(iniciarSesion(correo, clave), "login " + correo, 200).texto("$.accessToken");
	}

	public String admin() {
		if (tokenAdmin == null) {
			Resp login = exigir(iniciarSesion(ADMIN_CORREO, ADMIN_CLAVE), "login administrador", 200);
			tokenAdmin = login.texto("$.accessToken");
			if (Boolean.TRUE.equals(login.json("$.cambioPasswordRequerido"))) {
				exigir(put("/usuarios/" + ADMIN_CORREO, tokenAdmin, """
						{"nombre":"Administrador","correo":"%s","password":"%s","passwordActual":"%s"}
						""".formatted(ADMIN_CORREO, ADMIN_CLAVE, ADMIN_CLAVE)), "cambiar temporal inicial", 200);
				tokenAdmin = login(ADMIN_CORREO, ADMIN_CLAVE);
			}
		}
		return tokenAdmin;
	}


	public Cuenta solicitante() {
		int n = CONTADOR.incrementAndGet();
		return solicitante("sol%d@test.cl".formatted(n), rut(n), "Empresa " + n);
	}

	public Cuenta solicitante(String correo, String rutEmpresa, String razonSocial) {
		exigir(post("/solicitantes", null, cuerpoSolicitante(correo, rutEmpresa, razonSocial)),
				"registrar solicitante " + correo, 201);
		return new Cuenta(correo, login(correo, CLAVE));
	}

	public String cuerpoSolicitante(String correo, String rutEmpresa, String razonSocial) {
		return """
				{"nombre":"Solicitante","correo":"%s","telefono":"+56911111111","password":"%s",\
				"razonSocial":"%s","rutEmpresa":"%s"}""".formatted(correo, CLAVE, razonSocial, rutEmpresa);
	}

	public Cuenta conductor() {
		String correo = "con%d@test.cl".formatted(CONTADOR.incrementAndGet());
		exigir(post("/conductores", admin(), cuerpoConductor(correo)), "crear conductor " + correo, 201);
		return new Cuenta(correo, login(correo, CLAVE));
	}

	public String cuerpoConductor(String correo) {
		return """
				{"nombre":"Conductor","correo":"%s","telefono":"+56933333333","password":"%s"}"""
				.formatted(correo, CLAVE);
	}


	public String movil(int capacidad) {
		String patente = nuevaPatente();
		exigir(post("/moviles", admin(), cuerpoMovil(patente, capacidad)), "crear móvil " + patente, 201);
		return patente;
	}

	public String cuerpoMovil(String patente, int capacidad) {
		return """
				{"patente":"%s","capacidad":%d}""".formatted(patente, capacidad);
	}

	public Resp habilitar(String patente, String correoConductor) {
		return post("/habilitaciones-movil-conductor", admin(), """
				{"patente":"%s","correoConductor":"%s"}""".formatted(patente, correoConductor));
	}

	public void habilitarOk(String patente, String correoConductor) {
		exigir(habilitar(patente, correoConductor), "habilitar " + correoConductor + " en " + patente, 200, 201);
	}

	public Flota flota(int capacidad) {
		Cuenta conductor = conductor();
		String patente = movil(capacidad);
		habilitarOk(patente, conductor.correo());
		return new Flota(conductor, patente);
	}

	public record Flota(Cuenta conductor, String patente) {
	}

	public String estadoConductor(String correo) {
		return exigir(get("/conductores/" + correo, admin()), "consultar conductor", 200).texto("$.estadoConductor");
	}

	public String codigoAbordaje(String patente) {
		return jdbc.queryForObject("SELECT codigo_abordaje FROM movil WHERE patente = ?", String.class, patente);
	}

	public String estadoMovil(String patente) {
		return exigir(get("/moviles/" + patente, admin()), "consultar móvil", 200).texto("$.estadoMovil");
	}


	public Resp pasajero(String token, String dni, String nombre) {
		return post("/pasajeros", token, """
				{"tipoDocumento":"RUT","dni":"%s","nombre":"%s"}""".formatted(dni, nombre));
	}

	public String rutUnico() {
		String cuerpo = "%d".formatted(10_000_000 + CONTADOR.incrementAndGet());
		return cuerpo + "-" + Reglas.digitoVerificador(cuerpo);
	}

	public String pasajero(Cuenta solicitante) {
		return pasajeroConRut(solicitante, rutUnico());
	}

	public String pasajeroConRut(Cuenta solicitante, String rut) {
		return exigir(pasajero(solicitante.token(), rut, "Pasajero"), "crear pasajero", 200, 201).texto("$.dniHash");
	}

	public Resp abordar(String patente, String codigoAbordaje, String tipoDocumento, String dni) {
		return post("/abordajes", null, """
				{"patente":"%s","codigoAbordaje":"%s","tipoDocumento":"%s","dni":"%s"}"""
				.formatted(patente, codigoAbordaje, tipoDocumento, dni));
	}

	public Resp agregarPasajero(String token, String folio, String dniHash) {
		return post("/pasajeros-viaje", token, """
				{"folio":"%s","dniHash":"%s"}""".formatted(folio, dniHash));
	}

	public void agregarPasajeroOk(Cuenta solicitante, String folio, String dniHash) {
		exigir(agregarPasajero(solicitante.token(), folio, dniHash), "agregar pasajero al viaje", 200, 201);
	}


	public String cuerpoViaje(LocalDateTime fhAgenda, String comunaOrigen) {
		return """
				{"fhAgenda":"%s","numeroVuelo":"LA800","origen":%s,"destino":%s}""".formatted(
				FECHA.format(fhAgenda), ubicacion("Av. Providencia 1234", comunaOrigen, "-33.426100", "-70.611000"),
				ubicacion("Av. Apoquindo 4500", "Las Condes", "-33.412500", "-70.575000"));
	}

	private static String ubicacion(String direccion, String comuna, String latitud, String longitud) {
		return """
				{"direccion":"%s","comuna":"%s","region":"Metropolitana de Santiago",\
				"latitud":%s,"longitud":%s}""".formatted(direccion, comuna, latitud, longitud);
	}

	public String viaje(Cuenta solicitante) {
		return viaje(solicitante, LocalDateTime.now().plusDays(3));
	}

	public String viaje(Cuenta solicitante, LocalDateTime fhAgenda) {
		return exigir(post("/viajes", solicitante.token(), cuerpoViaje(fhAgenda, "Providencia")),
				"crear viaje", 201).texto("$.folio");
	}

	public void conPasajero(Cuenta solicitante, String folio) {
		agregarPasajeroOk(solicitante, folio, pasajero(solicitante));
	}

	public Resp verViaje(String folio) {
		return get("/viajes/" + folio, admin());
	}

	public String estadoViaje(String folio) {
		return exigir(verViaje(folio), "consultar viaje " + folio, 200).texto("$.estado");
	}

	public Resp asignar(String folio, String correoConductor, String patente) {
		return post("/viajes/" + folio + "/asignar", admin(), """
				{"correoConductor":"%s","patente":"%s"}""".formatted(correoConductor, patente));
	}

	public void asignarOk(String folio, String correoConductor, String patente) {
		exigir(asignar(folio, correoConductor, patente), "asignar " + folio, 200);
	}

	public Resp cambiarEstado(String folio, String token, String estado) {
		return post("/viajes/" + folio + "/estado", token, """
				{"estado":"%s"}""".formatted(estado));
	}

	public void cambiarEstadoOk(String folio, Cuenta conductor, String estado) {
		exigir(cambiarEstado(folio, conductor.token(), estado), "pasar " + folio + " a " + estado, 200);
	}

	public Resp cambiarEstadoEnLaVentana(String folio, String token, String estado) {
		LocalDateTime original = jdbc.queryForObject("SELECT fh_agenda FROM viaje WHERE folio = ?",
				LocalDateTime.class, folio);
		fijarAgenda(folio, LocalDateTime.now(ClockConfig.ZONA));
		try {
			return cambiarEstado(folio, token, estado);
		} finally {
			fijarAgenda(folio, original);
		}
	}

	public void iniciarOk(String folio, Cuenta conductor) {
		exigir(cambiarEstadoEnLaVentana(folio, conductor.token(), "EN_CURSO"), "iniciar " + folio, 200);
	}

	public void fijarAgenda(String folio, LocalDateTime fhAgenda) {
		assertThat(jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", fhAgenda, folio)).isEqualTo(1);
	}

	public void vencer(String folio) {
		fijarAgenda(folio, LocalDateTime.now(ClockConfig.ZONA).minusHours(1).minusMinutes(5));
	}

	public Resp cancelar(String folio, String token) {
		return post("/viajes/" + folio + "/cancelar", token, null);
	}

	public Resp cancelarComoAdmin(String folio, String motivo, Integer montoReembolso) {
		String monto = montoReembolso == null ? "" : ",\"montoReembolso\":" + montoReembolso;
		return post("/viajes/" + folio + "/cancelar", admin(), "{\"motivo\":\"%s\"%s}".formatted(motivo, monto));
	}

	public void cancelarComoAdminOk(String folio, String motivo, Integer montoReembolso) {
		exigir(cancelarComoAdmin(folio, motivo, montoReembolso), "cancelar " + folio + " como Administrador", 200);
	}

	public Resp rechazar(String folio, String token, String motivo) {
		return post("/viajes/" + folio + "/rechazar", token, "{\"motivo\":\"%s\"}".formatted(motivo));
	}

	public void rechazarOk(String folio, Cuenta conductor, String motivo) {
		exigir(rechazar(folio, conductor.token(), motivo), "rechazar " + folio, 200);
	}

	public void cancelarOk(String folio, Cuenta solicitante) {
		exigir(cancelar(folio, solicitante.token()), "cancelar " + folio, 200);
	}

	public String viajeAsignado(Cuenta solicitante, Flota flota) {
		return viajeAsignado(solicitante, flota, LocalDateTime.now().plusDays(3));
	}

	public String viajeAsignado(Cuenta solicitante, Flota flota, LocalDateTime fhAgenda) {
		String folio = viaje(solicitante, fhAgenda);
		conPasajero(solicitante, folio);
		asignarOk(folio, flota.conductor().correo(), flota.patente());
		return folio;
	}


	public Resp pagar(String token, String folio, int codSbif, String medioPago, int monto) {
		return post("/pagos", token, """
				{"folio":"%s","monto":%d,"codSbif":%d,"medioPago":"%s","tipoRecibo":"BOLETA"}"""
				.formatted(folio, monto, codSbif, medioPago));
	}

	public String pagarOk(String folio) {
		return exigir(pagar(admin(), folio, 12, "TRANSFERENCIA", 50_000), "registrar pago de " + folio, 201)
				.texto("$.hash");
	}

	public Resp cambiarEstadoPago(String hash, String token, String estado) {
		return post("/pagos/" + hash + "/estado", token, """
				{"estado":"%s"}""".formatted(estado));
	}

	public String estadoPago(String hash) {
		return exigir(get("/pagos/" + hash, admin()), "consultar pago", 200).texto("$.estadoPago");
	}


	private static String rut(int n) {
		String cuerpo = "%d%03d%03d".formatted(70 + n % 20, n / 1000 % 1000, n % 1000);
		return cuerpo + "-" + Reglas.digitoVerificador(cuerpo);
	}

	private static String nuevaPatente() {
		int n = CONTADOR.incrementAndGet();
		return "TS%c%c%02d".formatted((char) ('A' + n / 100 % 26), (char) ('A' + n / 2600 % 26), n % 100);
	}
}
