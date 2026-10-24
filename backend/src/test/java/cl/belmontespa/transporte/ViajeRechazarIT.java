package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V8 (README, Máquinas de estado): el conductor asignado rechaza un viaje ASIGNADO con un motivo y el viaje vuelve
 * a PENDIENTE. Cada test arma sus propias cuentas, vehículos y viajes, porque el esquema lo comparten todas las
 * clases de integración. Los push de V8 se prueban en NotificacionesIT.
 */
@DisplayName("Rechazar una asignación (HU25, V8)")
class ViajeRechazarIT extends IntegrationTest {

	private static final String MOTIVO = "Falla del vehículo";

	@Autowired
	private JdbcTemplate jdbc;

	private Resp ver(String folio) {
		return Api.exigir(api.verViaje(folio), "ver viaje " + folio, 200);
	}

	private void assertConflicto(Resp resp, String fragmento) {
		assertThat(resp.status()).as(resp.cuerpo()).isEqualTo(409);
		assertThat(resp.texto("$.message")).contains(fragmento);
	}

	@Nested
	@DisplayName("Efectos")
	class Efectos {

		@Test
		@DisplayName("vuelve a PENDIENTE sin conductor, vehículo ni fecha de asignación, y guarda el motivo recortado")
		void devuelveElViajeAPendiente() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);
			String agenda = ver(folio).texto("$.fhAgenda");

			Resp respuesta = Api.exigir(api.rechazar(folio, flota.conductor().token(), "  " + MOTIVO + "  "),
					"rechazar", 200);

			assertThat(respuesta.texto("$.estado")).isEqualTo("PENDIENTE");
			assertThat(respuesta.texto("$.motivo")).isEqualTo(MOTIVO);
			Resp guardado = ver(folio);
			assertThat(guardado.texto("$.estado")).isEqualTo("PENDIENTE");
			assertThat(guardado.texto("$.correoConductor")).isNull();
			assertThat(guardado.texto("$.nombreConductor")).isNull();
			assertThat(guardado.texto("$.patenteMovil")).isNull();
			assertThat(guardado.texto("$.fhAsignacion")).isNull();
			assertThat(guardado.texto("$.motivo")).isEqualTo(MOTIVO);
			assertThat(guardado.texto("$.fhAgenda")).isEqualTo(agenda);
			assertThat(guardado.entero("$.cantidadPasajeros")).isEqualTo(1);
		}

		@Test
		@DisplayName("sale de la agenda del conductor y no cambia el estado del conductor ni del vehículo")
		void saleDeLaAgendaYNoTocaLaFlota() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);

			api.rechazarOk(folio, flota.conductor(), MOTIVO);

			Resp agenda = api.get("/viajes", flota.conductor().token());
			assertThat(agenda.<List<String>>json("$[*].folio")).doesNotContain(folio);
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");
		}

		@Test
		@DisplayName("los pasajeros vuelven a Por abordar y siguen así al asignar el viaje de nuevo")
		void dejaPorAbordarALosPasajeros() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String rut = api.rutUnico();
			String folio = api.viaje(solicitante, LocalDateTime.now().plusDays(3));
			api.agregarPasajeroOk(solicitante, folio, api.pasajeroConRut(solicitante, rut));
			api.asignarOk(folio, flota.conductor().correo(), flota.patente());
			api.fijarAgenda(folio, LocalDateTime.now(ClockConfig.ZONA).plusMinutes(30));
			Api.exigir(api.abordar(flota.patente(), api.codigoAbordaje(flota.patente()), "RUT", rut), "abordar", 200);
			assertThat(ver(folio).<List<Object>>json("$.pasajeros[*].fhAbordaje")).doesNotContainNull();

			api.rechazarOk(folio, flota.conductor(), MOTIVO);

			assertThat(ver(folio).<List<Object>>json("$.pasajeros[*].fhAbordaje")).containsOnlyNulls();
			api.asignarOk(folio, otra.conductor().correo(), otra.patente());
			assertThat(ver(folio).<List<Object>>json("$.pasajeros[*].fhAbordaje")).containsOnlyNulls();
		}

		@Test
		@DisplayName("el pago no cambia")
		void noCambiaElPago() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);
			String hash = api.pagarOk(folio);
			Api.exigir(api.cambiarEstadoPago(hash, api.admin(), "PAGADO"), "marcar pagado", 200);

			api.rechazarOk(folio, flota.conductor(), MOTIVO);

			assertThat(api.estadoPago(hash)).isEqualTo("PAGADO");
			Resp pago = Api.exigir(api.get("/pagos/" + hash, api.admin()), "consultar pago", 200);
			assertThat(pago.texto("$.montoReembolsado")).isNull();
			assertThat(ver(folio).texto("$.estadoPago")).isEqualTo("PAGADO");
		}

		@Test
		@DisplayName("también se rechaza un viaje atrasado o vencido")
		void tambienVencido() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);
			api.vencer(folio);

			api.rechazarOk(folio, flota.conductor(), MOTIVO);

			assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("el Administrador lo asigna de nuevo y V2 limpia el motivo del rechazo")
		void seAsignaDeNuevo() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);
			api.rechazarOk(folio, flota.conductor(), MOTIVO);
			assertThat(ver(folio).texto("$.motivo")).isEqualTo(MOTIVO);

			api.asignarOk(folio, otra.conductor().correo(), otra.patente());

			Resp asignado = ver(folio);
			assertThat(asignado.texto("$.estado")).isEqualTo("ASIGNADO");
			assertThat(asignado.texto("$.correoConductor")).isEqualTo(otra.conductor().correo());
			assertThat(asignado.texto("$.patenteMovil")).isEqualTo(otra.patente());
			assertThat(asignado.texto("$.fhAsignacion")).isNotNull();
			assertThat(asignado.texto("$.motivo")).isNull();
		}

		@Test
		@DisplayName("queda en el historial: estado, conductor, vehículo y motivo")
		void quedaEnElHistorial() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);

			api.rechazarOk(folio, flota.conductor(), MOTIVO);

			List<Map<String, Object>> historial = jdbc.queryForList("""
					SELECT h.campo_modificado, h.valor
					  FROM historial_viaje h JOIN viaje v ON v.id_viaje = h.id_viaje
					 WHERE v.folio = ? ORDER BY h.id_historial DESC LIMIT 4""", folio);
			assertThat(historial).extracting(fila -> fila.get("campo_modificado"), fila -> fila.get("valor"))
					.contains(tuple("estado", "PENDIENTE"), tuple("conductor", "(sin valor)"),
							tuple("movil", "(sin valor)"), tuple("motivo", MOTIVO));
		}
	}

	@Nested
	@DisplayName("Errores")
	class Errores {

		@Test
		@DisplayName("400 si el motivo falta, está en blanco, tiene menos de 5 o más de 200 caracteres")
		void motivoInvalido() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);
			String token = flota.conductor().token();

			assertThat(api.post("/viajes/" + folio + "/rechazar", token, "{}").status()).isEqualTo(400);
			assertThat(api.post("/viajes/" + folio + "/rechazar", token, null).status()).isEqualTo(400);
			for (String motivo : List.of("", "     ", "abcd", "  abcd  ", "x".repeat(201))) {
				Resp resp = api.rechazar(folio, token, motivo);
				assertThat(resp.status()).as("motivo '%s': %s", motivo, resp.cuerpo()).isEqualTo(400);
				assertThat(resp.texto("$.message")).startsWith("motivo: ");
			}
			assertThat(api.rechazar(folio, token, "x".repeat(200)).status()).isEqualTo(200);
		}

		@Test
		@DisplayName("el 400 deja el viaje ASIGNADO con su conductor y su vehículo")
		void unRechazoInvalidoNoCambiaNada() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);

			assertThat(api.rechazar(folio, flota.conductor().token(), "abc").status()).isEqualTo(400);

			Resp viaje = ver(folio);
			assertThat(viaje.texto("$.estado")).isEqualTo("ASIGNADO");
			assertThat(viaje.texto("$.correoConductor")).isEqualTo(flota.conductor().correo());
			assertThat(viaje.texto("$.motivo")).isNull();
		}

		@Test
		@DisplayName("401 sin sesión; 403 para el Administrador, el Solicitante y otro conductor")
		void soloElConductorAsignado() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);

			assertThat(api.rechazar(folio, null, MOTIVO).status()).isEqualTo(401);
			assertThat(api.rechazar(folio, api.admin(), MOTIVO).status()).isEqualTo(403);
			assertThat(api.rechazar(folio, solicitante.token(), MOTIVO).status()).isEqualTo(403);
			assertThat(api.rechazar(folio, otra.conductor().token(), MOTIVO).status()).isEqualTo(403);

			assertThat(api.estadoViaje(folio)).isEqualTo("ASIGNADO");
		}

		@Test
		@DisplayName("404 si el viaje no existe")
		void viajeInexistente() {
			Flota flota = api.flota(4);

			assertThat(api.rechazar("TR-99999999", flota.conductor().token(), MOTIVO).status()).isEqualTo(404);
			assertThat(api.rechazar("NOEXISTE", flota.conductor().token(), MOTIVO).status()).isEqualTo(404);
		}

		@Test
		@DisplayName("un viaje PENDIENTE ya no es del conductor: 403, también al repetir el rechazo")
		void pendienteYRepetido() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String sinAsignar = api.viaje(solicitante);
			String folio = api.viajeAsignado(solicitante, flota);

			assertThat(api.rechazar(sinAsignar, flota.conductor().token(), MOTIVO).status()).isEqualTo(403);
			api.rechazarOk(folio, flota.conductor(), MOTIVO);
			assertThat(api.rechazar(folio, flota.conductor().token(), MOTIVO).status()).isEqualTo(403);
		}

		@Test
		@DisplayName("409 si el viaje está EN_CURSO, FINALIZADO o CANCELADO")
		void estadoQueNoSeRechaza() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String enCurso = api.viajeAsignado(solicitante, flota);
			api.iniciarOk(enCurso, flota.conductor());

			assertConflicto(api.rechazar(enCurso, flota.conductor().token(), MOTIVO), "EN_CURSO");
			api.cambiarEstadoOk(enCurso, flota.conductor(), "FINALIZADO");
			assertConflicto(api.rechazar(enCurso, flota.conductor().token(), MOTIVO), "FINALIZADO");

			// Cancelar conserva la asignación (V5): el conductor sigue siendo el del viaje, pero ya no hay qué rechazar.
			Flota otra = api.flota(4);
			String cancelado = api.viajeAsignado(solicitante, otra);
			api.cancelarOk(cancelado, solicitante);
			assertConflicto(api.rechazar(cancelado, otra.conductor().token(), MOTIVO), "CANCELADO");
			assertThat(api.estadoViaje(cancelado)).isEqualTo("CANCELADO");
		}
	}
}
