package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("Asignar y reasignar (HU05, V2)")
class ViajeAsignarIT extends IntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	private final LocalDateTime base = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0)
			.withNano(0);

	private Resp ver(String folio) {
		return Api.exigir(api.verViaje(folio), "ver viaje " + folio, 200);
	}

	private String viajeConPasajero(Cuenta solicitante, LocalDateTime fhAgenda) {
		String folio = api.viaje(solicitante, fhAgenda);
		api.conPasajero(solicitante, folio);
		return folio;
	}

	private void assertConflicto(Resp resp, String fragmento) {
		assertThat(resp.status()).as(resp.cuerpo()).isEqualTo(409);
		assertThat(resp.texto("$.message")).contains(fragmento);
	}

	private void fijarAgenda(String folio, LocalDateTime fhAgenda) {
		assertThat(jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", fhAgenda, folio)).isEqualTo(1);
	}

	private Cuenta conductorHabilitadoEn(String patente) {
		Cuenta conductor = api.conductor();
		api.habilitarOk(patente, conductor.correo());
		return conductor;
	}

	private String movilHabilitadoPara(Cuenta conductor) {
		String patente = api.movil(4);
		api.habilitarOk(patente, conductor.correo());
		return patente;
	}

	@Nested
	@DisplayName("Reglas del viaje")
	class DelViaje {

		@Test
		@DisplayName("sin pasajeros no se asigna")
		void sinPasajeros() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viaje(ana, base);

			assertConflicto(api.asignar(folio, flota.conductor().correo(), flota.patente()), "no tiene pasajeros");

			assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
			api.conPasajero(ana, folio);
			api.asignarOk(folio, flota.conductor().correo(), flota.patente());
			assertThat(api.estadoViaje(folio)).isEqualTo("ASIGNADO");
		}

		@Test
		@DisplayName("un viaje atrasado se asigna hasta una hora después de su horario; vencido, no")
		void vencido() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			LocalDateTime ahora = LocalDateTime.now(ClockConfig.ZONA);
			String atrasado = viajeConPasajero(ana, base);
			String vencido = viajeConPasajero(ana, base);
			fijarAgenda(atrasado, ahora.minusMinutes(50));
			fijarAgenda(vencido, ahora.minusMinutes(70));

			assertConflicto(api.asignar(vencido, flota.conductor().correo(), flota.patente()), "vencido");
			assertThat(api.estadoViaje(vencido)).isEqualTo("PENDIENTE");
			api.asignarOk(atrasado, flota.conductor().correo(), flota.patente());
			assertThat(api.estadoViaje(atrasado)).isEqualTo("ASIGNADO");
		}

		@Test
		@DisplayName("un viaje EN_CURSO no se reasigna")
		void enCurso() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String folio = api.viajeAsignado(ana, flota, base);
			api.iniciarOk(folio, flota.conductor());

			assertConflicto(api.asignar(folio, otra.conductor().correo(), otra.patente()), "EN_CURSO");
		}

		@Test
		@DisplayName("un viaje ASIGNADO que se vence ya no se reasigna")
		void asignadoVencido() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String folio = api.viajeAsignado(ana, flota, base);
			fijarAgenda(folio, LocalDateTime.now(ClockConfig.ZONA).minusHours(2));

			assertConflicto(api.asignar(folio, otra.conductor().correo(), otra.patente()), "vencido");
			assertThat(ver(folio).texto("$.correoConductor")).isEqualTo(flota.conductor().correo());
		}

		@Test
		@DisplayName("el viaje asignado entra a la agenda del Conductor de ese día")
		void entraALaAgenda() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = viajeConPasajero(ana, base);
			String agenda = "/viajes?fecha=" + base.toLocalDate();
			String conductor = flota.conductor().token();
			assertThat(Api.exigir(api.get(agenda, conductor), "agenda", 200).<List<String>>json("$[*].folio"))
					.isEmpty();

			api.asignarOk(folio, flota.conductor().correo(), flota.patente());

			assertThat(Api.exigir(api.get(agenda, conductor), "agenda", 200).<List<String>>json("$[*].folio"))
					.containsExactly(folio);
		}

		@Test
		@DisplayName("el Conductor reemplazado pierde el detalle (403) y el viaje sale de su agenda")
		void elReemplazadoPierdeElAcceso() {
			Cuenta ana = api.solicitante();
			Flota primera = api.flota(4);
			Flota segunda = api.flota(4);
			String folio = viajeConPasajero(ana, base);
			api.asignarOk(folio, primera.conductor().correo(), primera.patente());
			String agenda = "/viajes?fecha=" + base.toLocalDate();
			assertThat(api.get("/viajes/" + folio, primera.conductor().token()).status()).isEqualTo(200);

			api.asignarOk(folio, segunda.conductor().correo(), segunda.patente());

			assertThat(api.get("/viajes/" + folio, primera.conductor().token()).status()).isEqualTo(403);
			assertThat(Api.exigir(api.get(agenda, primera.conductor().token()), "agenda", 200)
					.<List<String>>json("$[*].folio")).doesNotContain(folio);
			assertThat(api.get("/viajes/" + folio, segunda.conductor().token()).status()).isEqualTo(200);
			assertThat(Api.exigir(api.get(agenda, segunda.conductor().token()), "agenda", 200)
					.<List<String>>json("$[*].folio")).containsExactly(folio);
		}
	}

	@Nested
	@DisplayName("Conductor y móvil")
	class DeLaFlota {

		@Test
		@DisplayName("un conductor o un móvil inactivos no se asignan")
		void inactivos() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = viajeConPasajero(ana, base);

			jdbc.update("UPDATE conductor SET activo = 0 WHERE id_usuario = (SELECT id_usuario FROM usuario WHERE correo = ?)",
					flota.conductor().correo());
			assertConflicto(api.asignar(folio, flota.conductor().correo(), flota.patente()), "conductor no está activo");

			jdbc.update("UPDATE conductor SET activo = 1 WHERE id_usuario = (SELECT id_usuario FROM usuario WHERE correo = ?)",
					flota.conductor().correo());
			jdbc.update("UPDATE movil SET activo = 0 WHERE patente = ?", flota.patente());
			assertConflicto(api.asignar(folio, flota.conductor().correo(), flota.patente()), "móvil no está activo");

			assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("asignar no cambia el estado del conductor ni del móvil, y puede tener varios viajes")
		void variosViajes() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);

			String uno = api.viajeAsignado(ana, flota, base);
			String dos = api.viajeAsignado(ana, flota, base.plusHours(5));

			assertThat(api.estadoViaje(uno)).isEqualTo("ASIGNADO");
			assertThat(api.estadoViaje(dos)).isEqualTo("ASIGNADO");
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");
		}
	}

	@Nested
	@DisplayName("Solape de dos horas")
	class Solape {

		@Test
		@DisplayName("el mismo conductor no toma otro viaje a menos de 2 horas, antes ni después")
		void conductor() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String otroMovil = movilHabilitadoPara(flota.conductor());
			api.viajeAsignado(ana, flota, base);
			String despues = viajeConPasajero(ana, base.plusMinutes(119));
			String antes = viajeConPasajero(ana, base.minusMinutes(119));

			assertConflicto(api.asignar(despues, flota.conductor().correo(), otroMovil),
					"conductor ya tiene un viaje a menos de 2 horas");
			assertConflicto(api.asignar(antes, flota.conductor().correo(), otroMovil),
					"conductor ya tiene un viaje a menos de 2 horas");
			assertThat(api.estadoViaje(despues)).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("el mismo móvil no toma otro viaje a menos de 2 horas")
		void movil() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Cuenta otroConductor = conductorHabilitadoEn(flota.patente());
			api.viajeAsignado(ana, flota, base);
			String cercano = viajeConPasajero(ana, base.plusMinutes(119));

			assertConflicto(api.asignar(cercano, otroConductor.correo(), flota.patente()),
					"móvil ya tiene un viaje a menos de 2 horas");
			assertThat(api.estadoViaje(cercano)).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("a exactamente 2 horas ya se puede")
		void dosHorasExactas() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			api.viajeAsignado(ana, flota, base);

			String despues = viajeConPasajero(ana, base.plusMinutes(120));
			String antes = viajeConPasajero(ana, base.minusMinutes(120));
			api.asignarOk(despues, flota.conductor().correo(), flota.patente());
			api.asignarOk(antes, flota.conductor().correo(), flota.patente());

			assertThat(api.estadoViaje(despues)).isEqualTo("ASIGNADO");
			assertThat(api.estadoViaje(antes)).isEqualTo("ASIGNADO");
		}

		@Test
		@DisplayName("un viaje cancelado o finalizado no cuenta para el solape")
		void cerradosNoCuentan() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String cancelado = api.viajeAsignado(ana, flota, base);
			api.cancelarOk(cancelado, ana);
			String finalizado = api.viajeAsignado(ana, flota, base.plusMinutes(10));
			api.iniciarOk(finalizado, flota.conductor());
			api.cambiarEstadoOk(finalizado, flota.conductor(), "FINALIZADO");

			String cercano = viajeConPasajero(ana, base.plusMinutes(30));
			api.asignarOk(cercano, flota.conductor().correo(), flota.patente());

			assertThat(api.estadoViaje(cercano)).isEqualTo("ASIGNADO");
		}

		@Test
		@DisplayName("el propio viaje no cuenta contra sí mismo al reasignarlo")
		void sinContarseASiMismo() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String otroMovil = movilHabilitadoPara(flota.conductor());
			String folio = api.viajeAsignado(ana, flota, base);

			api.asignarOk(folio, flota.conductor().correo(), otroMovil);

			assertThat(ver(folio).texto("$.patenteMovil")).isEqualTo(otroMovil);
		}
	}

	@Nested
	@DisplayName("Reasignar solo valida lo que cambia (RF22)")
	class Reasignar {

		@Test
		@DisplayName("el conductor Ocupado que conserva el viaje puede cambiar de móvil")
		void mismoConductorOcupado() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String otroMovil = movilHabilitadoPara(flota.conductor());
			String folio = api.viajeAsignado(ana, flota, base);
			String enCurso = api.viajeAsignado(ana, flota, base.plusDays(3));
			api.iniciarOk(enCurso, flota.conductor());
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("OCUPADO");

			api.asignarOk(folio, flota.conductor().correo(), otroMovil);

			assertThat(ver(folio).texto("$.correoConductor")).isEqualTo(flota.conductor().correo());
			assertThat(ver(folio).texto("$.patenteMovil")).isEqualTo(otroMovil);
		}

		@Test
		@DisplayName("el móvil En uso que conserva el viaje puede cambiar de conductor, pero uno Ocupado no se elige")
		void mismoMovilEnUso() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Cuenta nuevo = conductorHabilitadoEn(flota.patente());
			Cuenta ocupado = conductorHabilitadoEn(flota.patente());
			String folio = api.viajeAsignado(ana, flota, base);
			String enCurso = api.viajeAsignado(ana, flota, base.plusDays(3));
			api.iniciarOk(enCurso, flota.conductor());
			Flota deOcupado = new Flota(ocupado, api.movil(4));
			api.habilitarOk(deOcupado.patente(), ocupado.correo());
			String otro = api.viajeAsignado(ana, deOcupado, base.plusDays(4));
			api.iniciarOk(otro, ocupado);

			assertConflicto(api.asignar(folio, ocupado.correo(), flota.patente()), "conductor no está disponible");
			api.asignarOk(folio, nuevo.correo(), flota.patente());

			assertThat(ver(folio).texto("$.correoConductor")).isEqualTo(nuevo.correo());
			assertThat(ver(folio).texto("$.patenteMovil")).isEqualTo(flota.patente());
		}

		@Test
		@DisplayName("cambiar de móvil devuelve a los pasajeros a Por abordar; conservarlo no")
		void abordaje() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String otroMovil = movilHabilitadoPara(flota.conductor());
			Cuenta otroConductor = conductorHabilitadoEn(flota.patente());
			String folio = api.viaje(ana, base);
			api.conPasajero(ana, folio);
			api.conPasajero(ana, folio);
			api.asignarOk(folio, flota.conductor().correo(), flota.patente());
			LocalDateTime abordaron = base.minusMinutes(5);
			jdbc.update("""
					UPDATE detalle_viaje_pasajero d JOIN viaje v ON v.id_viaje = d.id_viaje
					SET d.fh_abordaje = ? WHERE v.folio = ?""", abordaron, folio);

			api.asignarOk(folio, otroConductor.correo(), flota.patente());
			assertThat(ver(folio).<List<Object>>json("$.pasajeros[*].fhAbordaje")).hasSize(2).doesNotContainNull();

			api.asignarOk(folio, flota.conductor().correo(), otroMovil);
			assertThat(ver(folio).<List<Object>>json("$.pasajeros[*].fhAbordaje")).hasSize(2).containsOnlyNulls();
		}

		@Test
		@DisplayName("repetir la misma asignación no cambia nada")
		void sinCambios() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viajeAsignado(ana, flota, base);
			String antes = ver(folio).texto("$.fhAsignacion");
			jdbc.update("UPDATE viaje SET fh_asignacion = fh_asignacion - INTERVAL 1 HOUR WHERE folio = ?", folio);
			String marcada = ver(folio).texto("$.fhAsignacion");

			Resp repetida = api.asignar(folio, flota.conductor().correo(), flota.patente());

			assertThat(repetida.status()).isEqualTo(200);
			assertThat(ver(folio).texto("$.fhAsignacion")).isEqualTo(marcada).isNotEqualTo(antes);
		}
	}
}
