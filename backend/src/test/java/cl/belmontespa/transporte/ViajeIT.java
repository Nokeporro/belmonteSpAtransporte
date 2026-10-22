package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

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

@DisplayName("Viajes: solicitud, asignación, estados y cancelación")
class ViajeIT extends IntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	private Resp ver(String folio) {
		return Api.exigir(api.verViaje(folio), "consultar viaje " + folio, 200);
	}

	@Nested
	@DisplayName("Registrar solicitud (CU-01)")
	class Creacion {

		@Test
		@DisplayName("el viaje nace PENDIENTE, con folio TR-1001 o mayor, sin pasajeros, conductor ni móvil")
		void nace() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			Resp viaje = ver(folio);
			assertThat(folio).matches("TR-[1-9][0-9]{3,}");
			assertThat(viaje.texto("$.estado")).isEqualTo("PENDIENTE");
			assertThat(viaje.texto("$.correoSolicitante")).isEqualTo(solicitante.correo());
			assertThat(viaje.entero("$.cantidadPasajeros")).isZero();
			assertThat(viaje.texto("$.correoConductor")).isNull();
			assertThat(viaje.texto("$.patenteMovil")).isNull();
			assertThat(viaje.texto("$.origen.comuna")).isEqualTo("Providencia");
			assertThat(viaje.texto("$.destino.comuna")).isEqualTo("Las Condes");
		}

		@Test
		@DisplayName("rechaza una fecha pasada, una comuna que no existe y un cuerpo incompleto con 400")
		void datosInvalidos() {
			Cuenta solicitante = api.solicitante();
			String pasada = api.cuerpoViaje(LocalDateTime.now().minusDays(1), "Providencia");
			String comunaInexistente = api.cuerpoViaje(LocalDateTime.now().plusDays(3), "Narnia");
			assertThat(api.post("/viajes", solicitante.token(), pasada).status()).isEqualTo(400);
			assertThat(api.post("/viajes", solicitante.token(), comunaInexistente).status()).isEqualTo(400);
			assertThat(api.post("/viajes", solicitante.token(), "{\"fhAgenda\":\"2099-01-01T10:00:00\"}").status())
					.isEqualTo(400);
		}

		@Test
		@DisplayName("solo el solicitante crea viajes")
		void soloSolicitante() {
			String cuerpo = api.cuerpoViaje(LocalDateTime.now().plusDays(3), "Providencia");
			assertThat(api.post("/viajes", api.admin(), cuerpo).status()).isEqualTo(403);
			assertThat(api.post("/viajes", api.conductor().token(), cuerpo).status()).isEqualTo(403);
			assertThat(api.post("/viajes", null, cuerpo).status()).isEqualTo(401);
		}
	}

	@Nested
	@DisplayName("Ciclo del viaje (CU-04 y CU-05)")
	class Ciclo {

		@Test
		@DisplayName("PENDIENTE → ASIGNADO → EN_CURSO → FINALIZADO: Ocupado y En uso solo de V3 a V4")
		void cicloCompleto() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viaje(solicitante);
			api.conPasajero(solicitante, folio);

			api.asignarOk(folio, flota.conductor().correo(), flota.patente());
			Resp asignado = ver(folio);
			assertThat(asignado.texto("$.estado")).isEqualTo("ASIGNADO");
			assertThat(asignado.texto("$.correoConductor")).isEqualTo(flota.conductor().correo());
			assertThat(asignado.texto("$.patenteMovil")).isEqualTo(flota.patente());
			assertThat(asignado.texto("$.fhAsignacion")).isNotNull();
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");

			api.cambiarEstadoOk(folio, flota.conductor(), "EN_CURSO");
			assertThat(ver(folio).texto("$.estado")).isEqualTo("EN_CURSO");
			assertThat(ver(folio).texto("$.fhInicio")).isNotNull();
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("OCUPADO");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("EN_USO");

			api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");
			Resp finalizado = ver(folio);
			assertThat(finalizado.texto("$.estado")).isEqualTo("FINALIZADO");
			assertThat(finalizado.texto("$.fhFin")).isNotNull();
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");
		}

		@Test
		@DisplayName("el historial registra estado, conductor y móvil de cada cambio, pero no las fechas (trigger)")
		void historial() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);
			api.cambiarEstadoOk(folio, flota.conductor(), "EN_CURSO");
			api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");

			List<String> campos = jdbc.queryForList("""
					SELECT DISTINCT h.campo_modificado FROM historial_viaje h
					JOIN viaje v ON v.id_viaje = h.id_viaje WHERE v.folio = ?""", String.class, folio);
			assertThat(campos).contains("estado", "conductor", "movil")
					.doesNotContain("fh_asignacion", "fh_inicio", "fh_fin");
			List<String> estados = jdbc.queryForList("""
					SELECT h.valor FROM historial_viaje h JOIN viaje v ON v.id_viaje = h.id_viaje
					WHERE v.folio = ? AND h.campo_modificado = 'estado' ORDER BY h.id_historial""", String.class,
					folio);
			assertThat(estados).hasSize(3).last().isNotEqualTo(estados.getFirst());
		}

		@Test
		@DisplayName("reasignar un viaje ASIGNADO cambia conductor y móvil sin tocar su estado")
		void reasignacion() {
			Cuenta solicitante = api.solicitante();
			Flota primera = api.flota(4);
			Flota segunda = api.flota(4);
			String folio = api.viajeAsignado(solicitante, primera);

			api.asignarOk(folio, segunda.conductor().correo(), segunda.patente());

			assertThat(ver(folio).texto("$.correoConductor")).isEqualTo(segunda.conductor().correo());
			assertThat(ver(folio).texto("$.patenteMovil")).isEqualTo(segunda.patente());
			assertThat(api.estadoConductor(primera.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(primera.patente())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoConductor(segunda.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(segunda.patente())).isEqualTo("DISPONIBLE");
		}
	}

	@Nested
	@DisplayName("Asignar (CU-04)")
	class Asignacion {

		@Test
		@DisplayName("un conductor no habilitado para el móvil responde 409 y no cambia nada")
		void noHabilitado() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			Cuenta ajeno = api.conductor();
			String folio = api.viaje(solicitante);
			api.conPasajero(solicitante, folio);

			assertThat(api.asignar(folio, ajeno.correo(), flota.patente()).status()).isEqualTo(409);

			assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoConductor(ajeno.correo())).isEqualTo("DISPONIBLE");
		}

		@Test
		@DisplayName("un conductor o un móvil ocupados en otro viaje EN_CURSO responden 409")
		void ocupados() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			Cuenta otroConductor = api.conductor();
			api.habilitarOk(flota.patente(), otroConductor.correo());
			String otroMovil = api.movil(4);
			api.habilitarOk(otroMovil, flota.conductor().correo());
			String enCurso = api.viajeAsignado(solicitante, flota);
			api.cambiarEstadoOk(enCurso, flota.conductor(), "EN_CURSO");

			// Diez días después: no hay solape, solo el conductor y el móvil están en uso.
			String segundo = api.viaje(solicitante, LocalDateTime.now().plusDays(13));
			api.conPasajero(solicitante, segundo);
			assertThat(api.asignar(segundo, flota.conductor().correo(), otroMovil).status()).isEqualTo(409);
			assertThat(api.asignar(segundo, otroConductor.correo(), flota.patente()).status()).isEqualTo(409);
			assertThat(api.estadoViaje(segundo)).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("más pasajeros que la capacidad del móvil responde 409")
		void superaCapacidad() {
			Cuenta solicitante = api.solicitante();
			Flota chica = api.flota(2);
			Flota grande = api.flota(4);
			String folio = api.viaje(solicitante);
			for (int i = 0; i < 3; i++) {
				api.agregarPasajeroOk(solicitante, folio, api.pasajero(solicitante));
			}

			assertThat(api.asignar(folio, chica.conductor().correo(), chica.patente()).status()).isEqualTo(409);
			assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
			assertThat(api.estadoConductor(chica.conductor().correo())).isEqualTo("DISPONIBLE");

			api.asignarOk(folio, grande.conductor().correo(), grande.patente());
			assertThat(api.estadoViaje(folio)).isEqualTo("ASIGNADO");
		}

		@Test
		@DisplayName("solo el administrador asigna")
		void soloAdministrador() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viaje(solicitante);
			String cuerpo = """
					{"correoConductor":"%s","patente":"%s"}""".formatted(flota.conductor().correo(), flota.patente());
			assertThat(api.post("/viajes/" + folio + "/asignar", solicitante.token(), cuerpo).status())
					.isEqualTo(403);
			assertThat(api.post("/viajes/" + folio + "/asignar", flota.conductor().token(), cuerpo).status())
					.isEqualTo(403);
		}

		@Test
		@DisplayName("un viaje, conductor o móvil inexistente responde 404")
		void inexistentes() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viaje(solicitante);
			assertThat(api.asignar("NOEXISTE", flota.conductor().correo(), flota.patente()).status())
					.isEqualTo(404);
			assertThat(api.asignar(folio, "nadie-" + System.nanoTime() + "@test.cl", flota.patente()).status())
					.isEqualTo(404);
			assertThat(api.asignar(folio, flota.conductor().correo(), "ZZ0000").status()).isEqualTo(404);
		}

		@Test
		@DisplayName("no se asigna un viaje cancelado o finalizado")
		void viajeCerrado() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String cancelado = api.viaje(solicitante);
			api.cancelarOk(cancelado, solicitante);
			assertThat(api.asignar(cancelado, flota.conductor().correo(), flota.patente()).status()).isEqualTo(409);

			String finalizado = api.viajeAsignado(solicitante, flota);
			api.cambiarEstadoOk(finalizado, flota.conductor(), "EN_CURSO");
			api.cambiarEstadoOk(finalizado, flota.conductor(), "FINALIZADO");
			assertThat(api.asignar(finalizado, flota.conductor().correo(), flota.patente()).status()).isEqualTo(409);
		}
	}

	@Nested
	@DisplayName("Cambiar estado (CU-05)")
	class Estados {

		@Test
		@DisplayName("solo el conductor asignado cambia el estado")
		void soloElAsignado() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			Cuenta otro = api.conductor();
			String folio = api.viajeAsignado(solicitante, flota);

			assertThat(api.cambiarEstado(folio, otro.token(), "EN_CURSO").status()).isEqualTo(403);
			assertThat(api.cambiarEstado(folio, solicitante.token(), "EN_CURSO").status()).isEqualTo(403);
			assertThat(api.cambiarEstado(folio, api.admin(), "EN_CURSO").status()).isEqualTo(403);
			assertThat(api.estadoViaje(folio)).isEqualTo("ASIGNADO");
		}

		@Test
		@DisplayName("un viaje sin conductor no admite cambios de estado")
		void sinConductor() {
			String folio = api.viaje(api.solicitante());
			assertThat(api.cambiarEstado(folio, api.conductor().token(), "EN_CURSO").status()).isEqualTo(403);
		}

		@Test
		@DisplayName("no se salta ni se retrocede: ASIGNADO→FINALIZADO y FINALIZADO→EN_CURSO responden 409")
		void transicionesInvalidas() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);

			assertThat(api.cambiarEstado(folio, flota.conductor().token(), "FINALIZADO").status()).isEqualTo(409);
			api.cambiarEstadoOk(folio, flota.conductor(), "EN_CURSO");
			assertThat(api.cambiarEstado(folio, flota.conductor().token(), "EN_CURSO").status()).isEqualTo(409);
			api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");
			assertThat(api.cambiarEstado(folio, flota.conductor().token(), "EN_CURSO").status()).isEqualTo(409);
			assertThat(api.estadoViaje(folio)).isEqualTo("FINALIZADO");
		}

		@Test
		@DisplayName("solo acepta EN_CURSO y FINALIZADO (sin distinguir mayúsculas)")
		void estadosPermitidos() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);

			assertThat(api.cambiarEstado(folio, flota.conductor().token(), "CANCELADO").status()).isEqualTo(400);
			assertThat(api.cambiarEstado(folio, flota.conductor().token(), "PENDIENTE").status()).isEqualTo(400);
			assertThat(api.cambiarEstado(folio, flota.conductor().token(), "en_curso").status()).isEqualTo(200);
		}
	}

	@Nested
	@DisplayName("Cancelar (CU-08)")
	class Cancelacion {

		@Test
		@DisplayName("cancelar un viaje PENDIENTE lo deja CANCELADO con fecha de fin")
		void pendiente() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			api.cancelarOk(folio, solicitante);
			Resp viaje = ver(folio);
			assertThat(viaje.texto("$.estado")).isEqualTo("CANCELADO");
			assertThat(viaje.texto("$.fhFin")).isNotNull();
		}

		@Test
		@DisplayName("cancelar un viaje ASIGNADO libera al conductor y al móvil")
		void asignado() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(solicitante, flota);

			api.cancelarOk(folio, solicitante);

			assertThat(api.estadoViaje(folio)).isEqualTo("CANCELADO");
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");
		}

		@Test
		@DisplayName("no se cancela un viaje EN_CURSO, FINALIZADO ni ya cancelado (409)")
		void noCancelable() {
			Cuenta solicitante = api.solicitante();
			Flota flota = api.flota(4);
			String enCurso = api.viajeAsignado(solicitante, flota);
			api.cambiarEstadoOk(enCurso, flota.conductor(), "EN_CURSO");
			assertThat(api.cancelar(enCurso, solicitante.token()).status()).isEqualTo(409);

			api.cambiarEstadoOk(enCurso, flota.conductor(), "FINALIZADO");
			assertThat(api.cancelar(enCurso, solicitante.token()).status()).isEqualTo(409);

			String cancelado = api.viaje(solicitante);
			api.cancelarOk(cancelado, solicitante);
			assertThat(api.cancelar(cancelado, solicitante.token()).status()).isEqualTo(409);
		}

		@Test
		@DisplayName("solo el solicitante dueño cancela; un viaje inexistente responde 404")
		void soloElDueno() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(ana, flota);

			assertThat(api.cancelar(folio, beto.token()).status()).isEqualTo(403);
			assertThat(api.cancelar(folio, flota.conductor().token()).status()).isEqualTo(403);
			assertThat(api.cancelar(folio, api.admin()).status()).isEqualTo(403);
			assertThat(api.cancelar("NOEXISTE", ana.token()).status()).isEqualTo(404);
			assertThat(api.estadoViaje(folio)).isEqualTo("ASIGNADO");
		}
	}

	@Nested
	@DisplayName("Consultas: cada rol ve solo lo suyo (RF03, RF04, RF07, RF08)")
	class Consulta {

		@Test
		@DisplayName("el detalle lo ven el administrador, el solicitante dueño y el conductor asignado")
		void detalle() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(ana, flota);
			assertThat(api.get("/viajes/" + folio, api.admin()).status()).isEqualTo(200);
			assertThat(api.get("/viajes/" + folio, ana.token()).status()).isEqualTo(200);
			assertThat(api.get("/viajes/" + folio, flota.conductor().token()).status()).isEqualTo(200);
		}

		@Test
		@DisplayName("otro solicitante o conductor reciben 403; un folio inexistente, 404")
		void ajeno() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(ana, flota);
			assertThat(api.get("/viajes/" + folio, beto.token()).status()).isEqualTo(403);
			assertThat(api.get("/viajes/" + folio, api.conductor().token()).status()).isEqualTo(403);
			assertThat(api.get("/viajes/NOEXISTE", api.admin()).status()).isEqualTo(404);
		}

		@Test
		@DisplayName("el solicitante lista solo sus viajes")
		void listaDelSolicitante() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			String deAna = api.viaje(ana);
			String deBeto = api.viaje(beto);
			Resp lista = Api.exigir(api.get("/viajes", ana.token()), "listar viajes", 200);
			assertThat(lista.<List<String>>json("$[*].folio")).contains(deAna).doesNotContain(deBeto);
			assertThat(lista.<List<String>>json("$[*].correoSolicitante")).containsOnly(ana.correo());
		}

		@Test
		@DisplayName("el administrador lista todos y filtra las solicitudes PENDIENTES")
		void listaDelAdministrador() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String pendiente = api.viaje(ana);
			String asignado = api.viajeAsignado(ana, flota);

			Resp todos = Api.exigir(api.get("/viajes", api.admin()), "listar todos", 200);
			assertThat(todos.<List<String>>json("$[*].folio")).contains(pendiente, asignado);

			Resp pendientes = Api.exigir(api.get("/viajes?estado=PENDIENTE", api.admin()), "listar pendientes", 200);
			assertThat(pendientes.<List<String>>json("$[*].folio")).contains(pendiente).doesNotContain(asignado);
			assertThat(pendientes.<List<String>>json("$[*].estado")).containsOnly("PENDIENTE");
		}

		@Test
		@DisplayName("el conductor lista los viajes asignados de un día y solo los suyos")
		void listaDelConductor() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String suyo = api.viajeAsignado(ana, flota);
			String deOtro = api.viajeAsignado(ana, otra);
			String fecha = LocalDate.now().plusDays(3).toString();

			Resp lista = Api.exigir(api.get("/viajes?fecha=" + fecha, flota.conductor().token()), "agenda", 200);
			assertThat(lista.<List<String>>json("$[*].folio")).contains(suyo).doesNotContain(deOtro);

			Resp hoy = Api.exigir(api.get("/viajes", flota.conductor().token()), "agenda de hoy", 200);
			assertThat(hoy.<List<String>>json("$[*].folio")).doesNotContain(suyo);
			assertThat(api.get("/viajes?fecha=ayer", flota.conductor().token()).status()).isEqualTo(400);
		}
	}
}
