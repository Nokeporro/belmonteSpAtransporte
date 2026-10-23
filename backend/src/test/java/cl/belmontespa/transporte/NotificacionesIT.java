package cl.belmontespa.transporte;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.FcmGrabador.Envio;
import cl.belmontespa.transporte.support.IntegrationTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Avisos externos: correo de recuperación (RF21) y push (RF12)")
class NotificacionesIT extends IntegrationTest {

	private int recuperar(String correo) {
		return api.post("/auth/recuperar-password", null, "{\"correo\":\"%s\"}".formatted(correo)).status();
	}

	private MimeMessage esperarUnCorreo() {
		assertThat(CORREO.waitForIncomingEmail(5000, 1)).as("llegó un correo").isTrue();
		return CORREO.getReceivedMessages()[0];
	}

	private List<Envio> esperarEnvios(int cantidad) {
		await().atMost(5, SECONDS).until(() -> fcm.envios().size() >= cantidad);
		return fcm.envios();
	}

	// El envío es asíncrono: para comprobar que algo NO sale hay que dejar pasar un tiempo.
	private static void dejarPasarEnvios() {
		await().pollDelay(Duration.ofMillis(400)).atMost(5, SECONDS).until(() -> true);
	}

	@Nested
	@DisplayName("Contraseña temporal por correo (CU-13)")
	class CorreoDeRecuperacion {

		@Test
		@DisplayName("el solicitante recibe una clave de 12 caracteres que sirve junto con la actual")
		void recibeLaClaveYPuedeEntrar() throws Exception {
			Cuenta ana = api.solicitante();

			assertThat(recuperar(ana.correo())).isEqualTo(204);

			MimeMessage correo = esperarUnCorreo();
			assertThat(correo.getAllRecipients()[0].toString()).isEqualTo(ana.correo());
			assertThat(correo.getFrom()[0].toString()).isEqualTo("no-reply@belmontespa.cl");
			assertThat(correo.getSubject()).isEqualTo("Belmonte SpA: tu contraseña temporal");
			String cuerpo = (String) correo.getContent();
			assertThat(cuerpo).contains("30 minutos");
			Matcher clave = Pattern.compile("(?m)^\\s+([A-Za-z0-9]{12})\\s*$").matcher(cuerpo);
			assertThat(clave.find()).as("el cuerpo trae la clave temporal").isTrue();

			assertThat(api.iniciarSesion(ana.correo(), clave.group(1)).status()).isEqualTo(200);
			assertThat(api.iniciarSesion(ana.correo(), Api.CLAVE).status()).isEqualTo(200);
			assertThat(api.iniciarSesion(ana.correo(), clave.group(1)).status()).isEqualTo(401);
		}

		@Test
		@DisplayName("la respuesta HTTP no contiene la clave")
		void respuestaSinClave() {
			Cuenta ana = api.solicitante();
			var resp = api.post("/auth/recuperar-password", null, "{\"correo\":\"%s\"}".formatted(ana.correo()));
			assertThat(resp.status()).isEqualTo(204);
			assertThat(resp.cuerpo()).isEmpty();
		}

		@Test
		@DisplayName("un correo inexistente o un conductor no reciben nada")
		void soloSolicitanteYAdministrador() throws Exception {
			Cuenta conductor = api.conductor();
			Cuenta control = api.solicitante();

			assertThat(recuperar("fantasma-" + System.nanoTime() + "@test.cl")).isEqualTo(204);
			assertThat(recuperar(conductor.correo())).isEqualTo(204);
			assertThat(recuperar(control.correo())).isEqualTo(204);

			MimeMessage unico = esperarUnCorreo();
			dejarPasarEnvios();
			assertThat(CORREO.getReceivedMessages()).hasSize(1);
			assertThat(unico.getAllRecipients()[0].toString()).isEqualTo(control.correo());
		}
	}

	@Nested
	@DisplayName("Push a los actores de un viaje (RF12/RF13)")
	class PushDeViajes {

		@Test
		@DisplayName("asignar avisa al conductor y al solicitante, con el folio del viaje")
		void asignar() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String tokenSolicitante = api.dispositivo(solicitante);
			String tokenConductor = api.dispositivo(flota.conductor());

			String folio = api.viajeAsignado(solicitante, flota);

			List<Envio> envios = esperarEnvios(2);
			assertThat(envios).hasSize(2);
			Envio alConductor = fcm.para(tokenConductor).getFirst();
			assertThat(alConductor.titulo()).isEqualTo("Nuevo viaje asignado");
			assertThat(alConductor.mensaje()).contains(folio);
			assertThat(alConductor.datos()).containsEntry("folio", folio);
			Envio alSolicitante = fcm.para(tokenSolicitante).getFirst();
			assertThat(alSolicitante.titulo()).isEqualTo("Viaje asignado");
			assertThat(alSolicitante.datos()).containsEntry("folio", folio);
		}

		@Test
		@DisplayName("reasignar a otro conductor avisa al reemplazado, con tipo AGENDA")
		void reasignarAvisaAlReemplazado() {
			Cuenta solicitante = api.solicitante();
			Flota primera = api.flota(4);
			Flota segunda = api.flota(4);
			api.dispositivo(solicitante);
			String tokenReemplazado = api.dispositivo(primera.conductor());
			String tokenNuevo = api.dispositivo(segunda.conductor());
			String folio = api.viajeAsignado(solicitante, primera);
			esperarEnvios(2);
			fcm.limpiar();

			api.asignarOk(folio, segunda.conductor().correo(), segunda.patente());

			esperarEnvios(3);
			Envio alReemplazado = fcm.para(tokenReemplazado).getFirst();
			assertThat(alReemplazado.titulo()).isEqualTo("Viaje reasignado");
			assertThat(alReemplazado.mensaje()).isEqualTo("El viaje " + folio + " fue asignado a otro conductor");
			assertThat(alReemplazado.datos()).containsEntry("tipo", "AGENDA").containsEntry("folio", folio);
			assertThat(fcm.para(tokenNuevo).getFirst().titulo()).isEqualTo("Nuevo viaje asignado");
		}

		@Test
		@DisplayName("reasignar solo el móvil no genera aviso de reemplazo: el conductor es el mismo")
		void reasignarSoloElMovil() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String otroMovil = api.movil(4);
			api.habilitarOk(otroMovil, flota.conductor().correo());
			api.dispositivo(solicitante);
			api.dispositivo(flota.conductor());
			String folio = api.viajeAsignado(solicitante, flota);
			esperarEnvios(2);
			fcm.limpiar();

			api.asignarOk(folio, flota.conductor().correo(), otroMovil);

			esperarEnvios(2);
			dejarPasarEnvios();
			assertThat(fcm.envios()).extracting(Envio::titulo)
					.containsExactlyInAnyOrder("Nuevo viaje asignado", "Viaje asignado");
		}

		@Test
		@DisplayName("repetir la misma asignación no avisa a nadie")
		void asignacionSinCambios() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			api.dispositivo(solicitante);
			api.dispositivo(flota.conductor());
			String folio = api.viajeAsignado(solicitante, flota);
			esperarEnvios(2);
			fcm.limpiar();

			api.asignarOk(folio, flota.conductor().correo(), flota.patente());

			dejarPasarEnvios();
			assertThat(fcm.envios()).isEmpty();
		}

		@Test
		@DisplayName("EN_CURSO y FINALIZADO avisan al solicitante")
		void cambioDeEstado() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String tokenSolicitante = api.dispositivo(solicitante);
			api.dispositivo(flota.conductor());
			String folio = api.viajeAsignado(solicitante, flota);
			esperarEnvios(2);
			fcm.limpiar();

			api.iniciarOk(folio, flota.conductor());
			Envio enCurso = esperarEnvios(1).getFirst();
			assertThat(enCurso.token()).isEqualTo(tokenSolicitante);
			assertThat(enCurso.mensaje()).contains("EN_CURSO");
			assertThat(enCurso.datos()).containsEntry("folio", folio);
			fcm.limpiar();

			api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");
			Envio finalizado = esperarEnvios(1).getFirst();
			assertThat(finalizado.token()).isEqualTo(tokenSolicitante);
			assertThat(finalizado.mensaje()).contains("FINALIZADO");
		}

		@Test
		@DisplayName("cancelar un viaje ASIGNADO avisa al conductor")
		void cancelarAsignado() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			api.dispositivo(solicitante);
			String tokenConductor = api.dispositivo(flota.conductor());
			String folio = api.viajeAsignado(solicitante, flota);
			esperarEnvios(2);
			fcm.limpiar();

			api.cancelarOk(folio, solicitante);

			Envio envio = esperarEnvios(1).getFirst();
			assertThat(envio.token()).isEqualTo(tokenConductor);
			assertThat(envio.titulo()).isEqualTo("Viaje cancelado");
			assertThat(envio.datos()).containsEntry("folio", folio);
		}

		@Test
		@DisplayName("cancelar un viaje PENDIENTE no avisa a nadie: no hay conductor")
		void cancelarPendiente() {
			Cuenta solicitante = api.solicitante();
			api.dispositivo(solicitante);
			api.cancelarOk(api.viaje(solicitante), solicitante);

			Cuenta control = api.solicitante();
			api.dispositivo(control);
			api.viajeAsignado(control, api.flota(4));
			esperarEnvios(1);
			dejarPasarEnvios();

			assertThat(fcm.envios()).extracting(Envio::titulo).doesNotContain("Viaje cancelado");
			assertThat(fcm.para("fcm-" + solicitante.correo())).isEmpty();
		}

		@Test
		@DisplayName("quien no registró un token FCM no recibe push")
		void sinToken() {
			Cuenta sinDispositivo = api.solicitante();
			Flota flotaSinDispositivo = api.flota(4);
			api.viajeAsignado(sinDispositivo, flotaSinDispositivo);

			Cuenta control = api.solicitante();
			String tokenControl = api.dispositivo(control);
			api.viajeAsignado(control, api.flota(4));
			esperarEnvios(1);
			dejarPasarEnvios();

			assertThat(fcm.envios()).extracting(Envio::token).containsOnly(tokenControl);
		}

		@Test
		@DisplayName("una asignación rechazada (409) no avisa a nadie")
		void asignacionRechazada() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			Cuenta noHabilitado = api.conductor();
			api.dispositivo(solicitante);
			api.dispositivo(noHabilitado);
			String rechazado = api.viaje(solicitante);

			assertThat(api.asignar(rechazado, noHabilitado.correo(), flota.patente()).status()).isEqualTo(409);

			Cuenta control = api.solicitante();
			api.dispositivo(control);
			String aceptado = api.viajeAsignado(control, api.flota(4));
			esperarEnvios(1);
			dejarPasarEnvios();

			assertThat(fcm.envios()).extracting(envio -> envio.datos().get("folio")).containsOnly(aceptado);
		}
	}
}
