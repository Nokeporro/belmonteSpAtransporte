package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * GET /viajes (README, "Consultas"): `estado` y `fecha` combinables para los tres roles, 400 con un
 * filtro mal formado, la agenda del Conductor y los campos de ViajeDTOResponse. El esquema lo
 * comparten todas las clases de integración, así que cada test crea sus propias cuentas y compara
 * solo los viajes que creó.
 */
@DisplayName("Consultas de viajes: filtros por rol, agenda del Conductor y campos del viaje (README, Consultas)")
class ViajeConsultasIT extends IntegrationTest {

	private static final LocalDate HOY = LocalDate.now(ClockConfig.ZONA);

	@Autowired
	private JdbcTemplate jdbc;

	private List<String> folios(String ruta, String token) {
		return Api.exigir(api.get(ruta, token), "listar " + ruta, 200).<List<String>>json("$[*].folio");
	}

	private String viajeEn(Cuenta solicitante, LocalDateTime fhAgenda) {
		return Api.exigir(api.post("/viajes", solicitante.token(), api.cuerpoViaje(fhAgenda, "Providencia")),
				"crear viaje", 201).texto("$.folio");
	}

	// La API no deja crear un viaje en el pasado: para probar los de ayer se mueve su fh_agenda.
	private void fijarAgenda(String folio, LocalDateTime fhAgenda) {
		assertThat(jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", fhAgenda, folio)).isEqualTo(1);
	}

	private static void assert400(Resp resp, String linea) {
		assertThat(resp.status()).as(resp.cuerpo()).isEqualTo(400);
		assertThat(resp.texto("$.message")).contains(linea);
	}

	@Nested
	@DisplayName("Administrador y Solicitante")
	class AdministradorYSolicitante {

		private final LocalDate dia = HOY.plusDays(12);
		private final LocalDate otroDia = HOY.plusDays(13);

		private Cuenta ana;
		private String pendienteDelDia;
		private String asignadoDelDia;
		private String pendienteDelOtroDia;

		private void preparar() {
			ana = api.solicitante();
			Flota flota = api.flota(4);
			pendienteDelDia = viajeEn(ana, dia.atTime(9, 0));
			asignadoDelDia = viajeEn(ana, dia.atTime(15, 0));
			api.conPasajero(ana, asignadoDelDia);
			api.asignarOk(asignadoDelDia, flota.conductor().correo(), flota.patente());
			pendienteDelOtroDia = viajeEn(ana, otroDia.atTime(9, 0));
		}

		@Test
		@DisplayName("el administrador combina estado y fecha")
		void administrador() {
			preparar();
			String admin = api.admin();

			assertThat(folios("/viajes?fecha=" + dia, admin)).contains(pendienteDelDia, asignadoDelDia)
					.doesNotContain(pendienteDelOtroDia);
			assertThat(folios("/viajes?estado=PENDIENTE&fecha=" + dia, admin)).contains(pendienteDelDia)
					.doesNotContain(asignadoDelDia, pendienteDelOtroDia);
			assertThat(folios("/viajes?estado=ASIGNADO&fecha=" + dia, admin)).contains(asignadoDelDia)
					.doesNotContain(pendienteDelDia, pendienteDelOtroDia);
			assertThat(folios("/viajes?estado=ASIGNADO&fecha=" + otroDia, admin))
					.doesNotContain(pendienteDelDia, asignadoDelDia, pendienteDelOtroDia);
			assertThat(folios("/viajes?estado=PENDIENTE", admin)).contains(pendienteDelDia, pendienteDelOtroDia)
					.doesNotContain(asignadoDelDia);
		}

		@Test
		@DisplayName("el solicitante ahora también filtra, y solo entre sus viajes")
		void solicitante() {
			preparar();
			Cuenta beto = api.solicitante();
			String deBeto = viajeEn(beto, dia.atTime(11, 0));

			assertThat(folios("/viajes?estado=PENDIENTE", ana.token()))
					.containsExactly(pendienteDelDia, pendienteDelOtroDia);
			assertThat(folios("/viajes?estado=ASIGNADO", ana.token())).containsExactly(asignadoDelDia);
			assertThat(folios("/viajes?fecha=" + otroDia, ana.token())).containsExactly(pendienteDelOtroDia);
			assertThat(folios("/viajes?estado=PENDIENTE&fecha=" + dia, ana.token())).containsExactly(pendienteDelDia);
			assertThat(folios("/viajes?estado=FINALIZADO", ana.token())).isEmpty();
			assertThat(folios("/viajes?fecha=" + dia, ana.token())).doesNotContain(deBeto);
			assertThat(folios("/viajes?estado=PENDIENTE", beto.token())).containsExactly(deBeto);
		}

		@Test
		@DisplayName("un parámetro que no es filtro se ignora y uno en blanco cuenta como ausente")
		void ignorados() {
			preparar();
			assertThat(folios("/viajes?foo=bar", ana.token())).hasSize(3);
			assertThat(folios("/viajes?estado=&fecha=", ana.token())).hasSize(3);
			assertThat(folios("/viajes?foo=bar&estado=PENDIENTE", api.admin())).contains(pendienteDelDia);
		}
	}

	@Nested
	@DisplayName("Un filtro mal formado responde 400 a los tres roles")
	class FiltroMalFormado {

		@Test
		@DisplayName("estado desconocido o en minúsculas, y fechas que no existen")
		void tresRoles() {
			Cuenta solicitante = api.solicitante();
			Cuenta conductor = api.conductor();
			for (String token : List.of(api.admin(), solicitante.token(), conductor.token())) {
				assert400(api.get("/viajes?estado=XYZ", token), "estado: Debe ser PENDIENTE");
				assert400(api.get("/viajes?estado=pendiente", token), "estado: ");
				assert400(api.get("/viajes?fecha=ayer", token), "fecha: Debe tener el formato AAAA-MM-DD.");
				assert400(api.get("/viajes?fecha=2026-13-01", token), "fecha: ");
				assert400(api.get("/viajes?estado=XYZ&fecha=ayer", token), "estado: ");
			}
		}

		@Test
		@DisplayName("el 400 solo se evalúa tras la autorización: sin token es 401")
		void sinToken() {
			assertThat(api.get("/viajes?estado=XYZ", null).status()).isEqualTo(401);
		}
	}

	@Nested
	@DisplayName("Agenda del Conductor")
	class Agenda {

		@Test
		@DisplayName("sin filtros trae su EN_CURSO de ayer, pero no un FINALIZADO de ayer")
		void enCursoAnterior() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String token = flota.conductor().token();
			String finalizado = api.viajeAsignado(ana, flota);
			api.cambiarEstadoOk(finalizado, flota.conductor(), "EN_CURSO");
			api.cambiarEstadoOk(finalizado, flota.conductor(), "FINALIZADO");
			fijarAgenda(finalizado, HOY.minusDays(1).atTime(8, 0));
			String enCurso = api.viajeAsignado(ana, flota);
			api.cambiarEstadoOk(enCurso, flota.conductor(), "EN_CURSO");
			fijarAgenda(enCurso, HOY.minusDays(1).atTime(22, 0));

			assertThat(folios("/viajes", token)).containsExactly(enCurso);
			assertThat(folios("/viajes?fecha=" + HOY, token)).containsExactly(enCurso);
			assertThat(folios("/viajes?fecha=" + HOY.minusDays(1), token)).containsExactly(finalizado, enCurso);
			assertThat(folios("/viajes?fecha=" + HOY.plusDays(1), token)).isEmpty();
		}

		@Test
		@DisplayName("la agenda de hoy trae lo asignado hoy y un viaje cancelado sale de ella")
		void agendaDeHoy() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String token = flota.conductor().token();
			String deHoy = api.viajeAsignado(ana, flota);
			fijarAgenda(deHoy, HOY.atTime(12, 0));

			assertThat(folios("/viajes", token)).containsExactly(deHoy);
			assertThat(folios("/viajes?fecha=" + HOY, token)).containsExactly(deHoy);

			api.cancelarOk(deHoy, ana);
			assertThat(folios("/viajes", token)).isEmpty();
		}

		@Test
		@DisplayName("con estado, trae sus viajes en ese estado de cualquier día, y la fecha se combina")
		void conEstado() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String token = flota.conductor().token();
			String folio = api.viajeAsignado(ana, flota);
			LocalDate agendado = LocalDate.parse(
					Api.exigir(api.verViaje(folio), "ver viaje", 200).texto("$.fhAgenda").substring(0, 10));

			assertThat(folios("/viajes?estado=ASIGNADO", token)).containsExactly(folio);
			assertThat(folios("/viajes?estado=FINALIZADO", token)).isEmpty();
			api.cambiarEstadoOk(folio, flota.conductor(), "EN_CURSO");
			api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");

			assertThat(folios("/viajes?estado=ASIGNADO", token)).isEmpty();
			assertThat(folios("/viajes?estado=FINALIZADO", token)).containsExactly(folio);
			assertThat(folios("/viajes?estado=FINALIZADO&fecha=" + agendado, token)).containsExactly(folio);
			assertThat(folios("/viajes?estado=FINALIZADO&fecha=" + agendado.plusDays(1), token)).isEmpty();
			// Un FINALIZADO de otro día no está en la agenda de hoy, pero sí en la de su día.
			assertThat(folios("/viajes", token)).isEmpty();
			assertThat(folios("/viajes?fecha=" + agendado, token)).containsExactly(folio);
		}
	}

	@Nested
	@DisplayName("Campos de ViajeDTOResponse")
	class Campos {

		private List<Object> campo(Resp lista, String folio, String campo) {
			return lista.<List<Object>>json("$[?(@.folio=='" + folio + "')]." + campo);
		}

		@Test
		@DisplayName("trae el solicitante, su empresa, el motivo y la hora de abordaje de cada pasajero")
		void solicitanteMotivoYAbordaje() {
			Cuenta ana = api.solicitante();
			String folio = api.viaje(ana);
			String abordado = api.pasajero(ana);
			String porAbordar = api.pasajero(ana);
			api.agregarPasajeroOk(ana, folio, abordado);
			api.agregarPasajeroOk(ana, folio, porAbordar);
			LocalDateTime abordo = HOY.atTime(14, 5);
			assertThat(jdbc.update("""
					UPDATE detalle_viaje_pasajero d
					JOIN viaje v ON v.id_viaje = d.id_viaje
					JOIN pasajero p ON p.id_pasajero = d.id_pasajero
					SET d.fh_abordaje = ? WHERE v.folio = ? AND p.dni_hash = ?""", abordo, folio, abordado))
					.isEqualTo(1);
			assertThat(jdbc.update("UPDATE viaje SET motivo = ? WHERE folio = ?", "Conductor sin disponibilidad",
					folio)).isEqualTo(1);

			Resp detalle = Api.exigir(api.verViaje(folio), "ver viaje", 200);
			assertThat(detalle.texto("$.correoSolicitante")).isEqualTo(ana.correo());
			assertThat(detalle.texto("$.nombreSolicitante")).isEqualTo("Solicitante");
			assertThat(detalle.texto("$.telefonoSolicitante")).isEqualTo("+56911111111");
			assertThat(detalle.texto("$.razonSocial")).startsWith("Empresa ");
			assertThat(detalle.texto("$.motivo")).isEqualTo("Conductor sin disponibilidad");
			assertThat(detalle.<List<String>>json("$.pasajeros[*].dniHash")).containsExactlyInAnyOrder(abordado,
					porAbordar);
			assertThat(LocalDateTime.parse(
					detalle.<List<String>>json("$.pasajeros[?(@.dniHash=='" + abordado + "')].fhAbordaje").get(0)))
					.isEqualTo(abordo);
			assertThat(detalle.<List<Object>>json("$.pasajeros[?(@.dniHash=='" + porAbordar + "')].fhAbordaje"))
					.containsExactly((Object) null);

			// La lista trae lo mismo, para los tres roles que la ven.
			Resp lista = Api.exigir(api.get("/viajes?estado=PENDIENTE", api.admin()), "listar", 200);
			assertThat(campo(lista, folio, "nombreSolicitante")).containsExactly("Solicitante");
			assertThat(campo(lista, folio, "telefonoSolicitante")).containsExactly("+56911111111");
			assertThat(campo(lista, folio, "motivo")).containsExactly("Conductor sin disponibilidad");
		}

		@Test
		@DisplayName("sin teléfono ni motivo llegan en null, no ausentes")
		void nulos() {
			Cuenta ana = api.solicitante();
			String folio = api.viaje(ana);
			jdbc.update("UPDATE usuario SET telefono = NULL WHERE correo = ?", ana.correo());

			Resp detalle = Api.exigir(api.verViaje(folio), "ver viaje", 200);
			assertThat(detalle.<Object>json("$.telefonoSolicitante")).isNull();
			assertThat(detalle.<Object>json("$.motivo")).isNull();
			assertThat(detalle.<List<Object>>json("$.pasajeros")).isEmpty();
		}

		@Test
		@DisplayName("el pago lo ven el administrador y el solicitante, y el conductor lo recibe en null")
		void pagoOcultoAlConductor() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(ana, flota);
			String hash = api.pagarOk(folio);
			String fecha = Api.exigir(api.verViaje(folio), "ver viaje", 200).texto("$.fhAgenda").substring(0, 10);

			for (String token : List.of(api.admin(), ana.token())) {
				Resp detalle = Api.exigir(api.get("/viajes/" + folio, token), "ver viaje", 200);
				assertThat(detalle.texto("$.hashPago")).isEqualTo(hash);
				assertThat(detalle.texto("$.estadoPago")).isEqualTo("PENDIENTE");
			}
			assertThat(campo(Api.exigir(api.get("/viajes?fecha=" + fecha, ana.token()), "listar", 200), folio,
					"hashPago")).containsExactly(hash);

			String conductor = flota.conductor().token();
			Resp detalle = Api.exigir(api.get("/viajes/" + folio, conductor), "ver viaje", 200);
			assertThat(detalle.<Object>json("$.hashPago")).isNull();
			assertThat(detalle.<Object>json("$.estadoPago")).isNull();
			Resp lista = Api.exigir(api.get("/viajes?estado=ASIGNADO", conductor), "listar", 200);
			assertThat(campo(lista, folio, "hashPago")).containsExactly((Object) null);
			assertThat(campo(lista, folio, "estadoPago")).containsExactly((Object) null);

			Resp cambio = Api.exigir(api.cambiarEstado(folio, conductor, "EN_CURSO"), "iniciar", 200);
			assertThat(cambio.<Object>json("$.hashPago")).isNull();
			assertThat(cambio.<Object>json("$.estadoPago")).isNull();
			// Lo que se oculta es la respuesta: el pago sigue ahí para el administrador.
			assertThat(Api.exigir(api.verViaje(folio), "ver viaje", 200).texto("$.hashPago")).isEqualTo(hash);
		}
	}
}
