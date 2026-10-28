package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("Iniciar y finalizar el viaje (HU09, V3 y V4)")
class ViajeEstadoIT extends IntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	private final LocalDateTime base = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0)
			.withNano(0);

	private static LocalDateTime ahora() {
		return LocalDateTime.now(ClockConfig.ZONA);
	}

	private void fijarAgenda(String folio, LocalDateTime fhAgenda) {
		assertThat(jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", fhAgenda, folio)).isEqualTo(1);
	}

	private Resp ver(String folio) {
		return Api.exigir(api.verViaje(folio), "ver viaje " + folio, 200);
	}

	private void assertConflicto(Resp resp, String fragmento) {
		assertThat(resp.status()).as(resp.cuerpo()).isEqualTo(409);
		assertThat(resp.texto("$.message")).contains(fragmento);
	}

	private String viajeConAgendaA(Cuenta solicitante, Flota flota, long minutos) {
		String folio = api.viajeAsignado(solicitante, flota);
		fijarAgenda(folio, ahora().plusMinutes(minutos));
		return folio;
	}

	@Nested
	@DisplayName("Ventana de inicio (± 1 h de la agenda)")
	class Ventana {

		@Test
		@DisplayName("fuera de la ventana, antes o después, responde 409 y no cambia nada")
		void fuera() {
			Cuenta ana = api.solicitante();
			for (long minutos : new long[] {61, -61}) {
				Flota flota = api.flota(4);
				String folio = viajeConAgendaA(ana, flota, minutos);

				assertConflicto(api.cambiarEstado(folio, flota.conductor().token(), "EN_CURSO"),
						"fuera de la ventana de inicio");

				assertThat(api.estadoViaje(folio)).isEqualTo("ASIGNADO");
				assertThat(ver(folio).<Object>json("$.fhInicio")).isNull();
				assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("DISPONIBLE");
				assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");
			}
		}

		@Test
		@DisplayName("dentro de la ventana, a 59 min antes, en punto o 59 min después, inicia")
		void dentro() {
			Cuenta ana = api.solicitante();
			for (long minutos : new long[] {59, 0, -59}) {
				Flota flota = api.flota(4);
				String folio = viajeConAgendaA(ana, flota, minutos);

				Resp inicio = Api.exigir(api.cambiarEstado(folio, flota.conductor().token(), "EN_CURSO"),
						"iniciar a " + minutos + " min", 200);

				assertThat(inicio.texto("$.estado")).isEqualTo("EN_CURSO");
				assertThat(inicio.texto("$.fhInicio")).isNotNull();
				assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("OCUPADO");
				assertThat(api.estadoMovil(flota.patente())).isEqualTo("EN_USO");
			}
		}

		@Test
		@DisplayName("finalizar no mira la ventana: un viaje En curso se cierra aunque su agenda quede lejos")
		void finalizarSinVentana() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = viajeConAgendaA(ana, flota, 0);
			api.cambiarEstadoOk(folio, flota.conductor(), "EN_CURSO");
			fijarAgenda(folio, ahora().minusDays(2));

			api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");

			assertThat(api.estadoViaje(folio)).isEqualTo("FINALIZADO");
			assertThat(ver(folio).texto("$.fhFin")).isNotNull();
			assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("DISPONIBLE");
			assertThat(api.estadoMovil(flota.patente())).isEqualTo("DISPONIBLE");
		}
	}

	@Nested
	@DisplayName("Un solo viaje En curso por conductor y por móvil")
	class Exclusividad {

		@Test
		@DisplayName("otro viaje En curso del conductor impide iniciar; al finalizarlo ya se puede")
		void delConductor() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String primero = api.viajeAsignado(ana, flota, base);
			String segundo = api.viajeAsignado(ana, flota, base.plusDays(1));
			api.iniciarOk(primero, flota.conductor());
			fijarAgenda(segundo, ahora());

			assertConflicto(api.cambiarEstado(segundo, flota.conductor().token(), "EN_CURSO"),
					"conductor ya tiene un viaje en curso");
			assertThat(api.estadoViaje(segundo)).isEqualTo("ASIGNADO");

			api.cambiarEstadoOk(primero, flota.conductor(), "FINALIZADO");
			api.cambiarEstadoOk(segundo, flota.conductor(), "EN_CURSO");
			assertThat(api.estadoViaje(segundo)).isEqualTo("EN_CURSO");
		}

		@Test
		@DisplayName("otro viaje En curso del móvil impide iniciar, aunque lo conduzca otra persona")
		void delMovil() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Cuenta otro = api.conductor();
			api.habilitarOk(flota.patente(), otro.correo());
			String primero = api.viajeAsignado(ana, flota, base);
			String segundo = api.viajeAsignado(ana, new Flota(otro, flota.patente()), base.plusDays(1));
			api.iniciarOk(primero, flota.conductor());
			fijarAgenda(segundo, ahora());

			assertConflicto(api.cambiarEstado(segundo, otro.token(), "EN_CURSO"), "móvil ya tiene un viaje en curso");
			assertThat(api.estadoViaje(segundo)).isEqualTo("ASIGNADO");
			assertThat(api.estadoConductor(otro.correo())).isEqualTo("DISPONIBLE");

			api.cambiarEstadoOk(primero, flota.conductor(), "FINALIZADO");
			api.cambiarEstadoOk(segundo, otro, "EN_CURSO");
			assertThat(api.estadoViaje(segundo)).isEqualTo("EN_CURSO");
		}

		@Test
		@DisplayName("el viaje En curso de otro conductor y otro móvil no bloquea")
		void deOtros() {
			Cuenta ana = api.solicitante();
			Flota una = api.flota(4);
			Flota otra = api.flota(4);
			api.iniciarOk(api.viajeAsignado(ana, una), una.conductor());
			String folio = viajeConAgendaA(ana, otra, 0);

			api.cambiarEstadoOk(folio, otra.conductor(), "EN_CURSO");

			assertThat(api.estadoViaje(folio)).isEqualTo("EN_CURSO");
		}

		@Test
		@DisplayName("la ventana se revisa antes que la exclusividad")
		void ventanaPrimero() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String primero = api.viajeAsignado(ana, flota, base);
			String segundo = api.viajeAsignado(ana, flota, base.plusDays(1));
			api.iniciarOk(primero, flota.conductor());

			assertConflicto(api.cambiarEstado(segundo, flota.conductor().token(), "EN_CURSO"),
					"fuera de la ventana de inicio");
		}
	}
}
