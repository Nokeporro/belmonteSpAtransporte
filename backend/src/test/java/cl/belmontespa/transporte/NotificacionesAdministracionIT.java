package cl.belmontespa.transporte;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.dto.request.SolicitanteDTORequest;
import cl.belmontespa.transporte.dto.request.ViajeDTORequest;
import cl.belmontespa.transporte.service.SolicitanteService;
import cl.belmontespa.transporte.service.ViajeService;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@DisplayName("HU02 y HU01: avisos a administración por solicitudes y cuentas pendientes")
class NotificacionesAdministracionIT extends IntegrationTest {
	@Autowired private ViajeService viajes;
	@Autowired private SolicitanteService solicitantes;
	@Autowired private PlatformTransactionManager transacciones;
	@Autowired private ObjectMapper json;

	private String correo(String prefijo) {
		return prefijo + "-" + System.nanoTime() + "@test.cl";
	}

	private Cuenta otroAdministrador() {
		String correo = correo("admin-avisos");
		Api.exigir(api.post("/administradores", api.admin(), """
				{"nombre":"Administración de prueba","correo":"%s","password":"%s"}
				""".formatted(correo, Api.CLAVE)), "crear administrador", 201);
		String temporal = api.login(correo, Api.CLAVE);
		Api.exigir(api.put("/usuarios/" + correo, temporal, """
				{"nombre":"Administración de prueba","correo":"%s","password":"%s","passwordActual":"%s"}
				""".formatted(correo, Api.CLAVE, Api.CLAVE)), "cambiar contraseña inicial", 200);
		return new Cuenta(correo, api.login(correo, Api.CLAVE));
	}

	private String rutEmpresa(Cuenta solicitante) {
		return Api.exigir(api.get("/solicitantes/" + solicitante.correo(), solicitante.token()),
				"consultar empresa", 200).texto("$.rutEmpresa");
	}

	private Api.Resp registrarPendiente(String rut, String correo) {
		return api.post("/solicitantes", null, api.cuerpoSolicitante(correo, rut, "Empresa existente"));
	}

	private void esperarEnvios(int cantidad) {
		await().atMost(5, SECONDS).until(() -> fcm.envios().size() >= cantidad);
		dejarPasarEnvios();
	}

	private static void dejarPasarEnvios() {
		await().pollDelay(Duration.ofMillis(400)).atMost(5, SECONDS).until(() -> true);
	}

	@Test
	void nuevaSolicitudAvisaATodosLosAdministradoresConTokenSinAvisarAOtrosRoles() {
		Cuenta solicitante = api.solicitante();
		Cuenta conductor = api.conductor();
		Cuenta otroAdmin = otroAdministrador();
		otroAdministrador();
		String tokenSolicitante = api.dispositivo(solicitante);
		String tokenConductor = api.dispositivo(conductor);
		String tokenAdmin = api.dispositivoDelAdministrador();
		String tokenOtroAdmin = api.dispositivo(otroAdmin);
		LocalDateTime agenda = LocalDateTime.now(ClockConfig.ZONA).plusHours(3).withSecond(0).withNano(0);

		String folio = api.viaje(solicitante, agenda);

		esperarEnvios(2);
		assertThat(fcm.envios()).hasSize(2);
		assertThat(fcm.envios()).extracting(envio -> envio.token()).containsExactlyInAnyOrder(tokenAdmin, tokenOtroAdmin);
		assertThat(fcm.envios()).allSatisfy(envio -> {
			assertThat(envio.titulo()).isEqualTo("Nueva solicitud");
			assertThat(envio.mensaje()).isEqualTo("Viaje " + folio + " para "
					+ DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm").format(agenda));
			assertThat(envio.datos()).containsOnlyKeys("tipo", "folio")
					.containsEntry("tipo", "VIAJE").containsEntry("folio", folio);
		});
		assertThat(fcm.para(tokenSolicitante)).isEmpty();
		assertThat(fcm.para(tokenConductor)).isEmpty();
	}

	@Test
	void cuentaPendienteAvisaATodosLosAdministradoresConTipoCuentasSinDatosPersonales() {
		Cuenta titular = api.solicitante();
		Cuenta otroAdmin = otroAdministrador();
		String rut = rutEmpresa(titular);
		String tokenTitular = api.dispositivo(titular);
		String tokenAdmin = api.dispositivoDelAdministrador();
		String tokenOtroAdmin = api.dispositivo(otroAdmin);

		var respuesta = Api.exigir(registrarPendiente(rut, correo("pendiente")), "registrar cuenta pendiente", 201);

		assertThat(respuesta.texto("$.estadoSolicitante")).isEqualTo("PENDIENTE");
		esperarEnvios(2);
		assertThat(fcm.envios()).hasSize(2);
		assertThat(fcm.envios()).extracting(envio -> envio.token()).containsExactlyInAnyOrder(tokenAdmin, tokenOtroAdmin);
		assertThat(fcm.envios()).allSatisfy(envio -> {
			assertThat(envio.titulo()).isEqualTo("Cuenta por aprobar");
			assertThat(envio.mensaje()).isEqualTo("Hay una cuenta nueva por revisar");
			assertThat(envio.datos()).containsOnlyKeys("tipo").containsEntry("tipo", "CUENTAS");
		});
		assertThat(fcm.para(tokenTitular)).isEmpty();
	}

	@Test
	void unaCuentaDeEmpresaNuevaSeApruebaSinAvisoDeRevision() {
		api.dispositivoDelAdministrador();

		Cuenta solicitante = api.solicitante();

		assertThat(api.get("/solicitantes/" + solicitante.correo(), solicitante.token())
				.texto("$.estadoSolicitante")).isEqualTo("APROBADO");
		dejarPasarEnvios();
		assertThat(fcm.envios()).isEmpty();
	}

	@Test
	void registrosInvalidosYCorreosRepetidosNoEnvianAvisos() {
		Cuenta titular = api.solicitante();
		String rut = rutEmpresa(titular);
		api.dispositivoDelAdministrador();

		assertThat(registrarPendiente(rut, titular.correo()).status()).isEqualTo(409);
		assertThat(api.post("/solicitantes", null, "{}").status()).isEqualTo(400);
		assertThat(api.post("/viajes", titular.token(), api.cuerpoViaje(
				LocalDateTime.now(ClockConfig.ZONA).plusMinutes(30), "Providencia")).status()).isEqualTo(400);

		dejarPasarEnvios();
		assertThat(fcm.envios()).isEmpty();
	}

	@Test
	void elRollbackDeUnViajeODeUnaCuentaPendienteNoEnviaAvisos() {
		Cuenta titular = api.solicitante();
		String rut = rutEmpresa(titular);
		api.dispositivoDelAdministrador();
		ViajeDTORequest viaje = json.readValue(api.cuerpoViaje(
				LocalDateTime.now(ClockConfig.ZONA).plusHours(3), "Providencia"), ViajeDTORequest.class);

		String folio = new TransactionTemplate(transacciones).execute(tx -> {
			var creado = viajes.crear(viaje, titular.correo());
			assertThat(fcm.envios()).isEmpty();
			tx.setRollbackOnly();
			return creado.folio();
		});
		String correo = correo("rollback");
		new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
			solicitantes.crear(new SolicitanteDTORequest("Persona de prueba", correo, null,
					Api.CLAVE, "Empresa existente", rut));
			assertThat(fcm.envios()).isEmpty();
			tx.setRollbackOnly();
		});

		dejarPasarEnvios();
		assertThat(fcm.envios()).isEmpty();
		assertThat(api.get("/viajes/" + folio, api.admin()).status()).isEqualTo(404);
		assertThat(api.get("/solicitantes/" + correo, api.admin()).status()).isEqualTo(404);
	}

	@Test
	void sinDispositivosDeAdministracionNoSeAvisaASolicitantesNiConductores() {
		Cuenta titular = api.solicitante();
		Cuenta conductor = api.conductor();
		String rut = rutEmpresa(titular);
		api.dispositivo(titular);
		api.dispositivo(conductor);

		api.viaje(titular);
		Api.exigir(registrarPendiente(rut, correo("sin-dispositivos")), "registrar cuenta pendiente", 201);

		dejarPasarEnvios();
		assertThat(fcm.envios()).isEmpty();
	}
}
