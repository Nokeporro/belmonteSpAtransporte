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

@DisplayName("Abordaje con código QR (HU29)")
class AbordajeIT extends IntegrationTest {

	private static final String NO_ENCONTRADO = "No encontramos un viaje para este documento en este vehículo";

	@Autowired
	private JdbcTemplate jdbc;

	private static LocalDateTime ahora() {
		return LocalDateTime.now(ClockConfig.ZONA);
	}

	private void fijarAgenda(String folio, LocalDateTime fhAgenda) {
		assertThat(jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", fhAgenda, folio)).isEqualTo(1);
	}

	private String viajeA(Cuenta solicitante, Flota flota, String rut, long minutos) {
		String folio = api.viaje(solicitante, LocalDateTime.now().plusDays(3 + Math.abs(rut.hashCode() % 20)));
		api.agregarPasajeroOk(solicitante, folio, api.pasajeroConRut(solicitante, rut));
		api.asignarOk(folio, flota.conductor().correo(), flota.patente());
		fijarAgenda(folio, ahora().plusMinutes(minutos));
		return folio;
	}

	private Resp abordar(Flota flota, String rut) {
		return api.abordar(flota.patente(), api.codigoAbordaje(flota.patente()), "RUT", rut);
	}

	private void assertNoEncontrado(Resp resp) {
		assertThat(resp.status()).as(resp.cuerpo()).isEqualTo(404);
		assertThat(resp.texto("$.message")).isEqualTo(NO_ENCONTRADO);
	}

	private String marca(String folio, String token, String dniHash) {
		Resp viaje = Api.exigir(api.get("/viajes/" + folio, token), "ver viaje " + folio, 200);
		List<Object> marcas = viaje.json("$.pasajeros[?(@.dniHash=='" + dniHash + "')].fhAbordaje");
		assertThat(marcas).hasSize(1);
		return marcas.getFirst() == null ? null : marcas.getFirst().toString();
	}

	@Nested
	@DisplayName("Registro")
	class Registro {

		@Test
		@DisplayName("es una ruta pública: sin token responde 200 con la hora de la marca")
		void publica() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String rut = api.rutUnico();
			viajeA(ana, flota, rut, 30);

			Resp resp = abordar(flota, rut);

			Api.exigir(resp, "abordar", 200);
			assertThat(resp.texto("$.fhAbordaje")).isNotNull();
			assertThat(resp.cuerpo()).doesNotContain(rut.substring(0, 8));
		}

		@Test
		@DisplayName("la marca la ven el Solicitante, el Conductor y el Administrador, solo en quien abordó")
		void marcaEnLosTresRoles() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String rut = api.rutUnico();
			String otroRut = api.rutUnico();
			String folio = viajeA(ana, flota, rut, 10);
			String otroHash = api.pasajeroConRut(ana, otroRut);
			api.agregarPasajeroOk(ana, folio, otroHash);
			String hash = api.pasajeroConRut(ana, rut);

			String hora = Api.exigir(abordar(flota, rut), "abordar", 200).texto("$.fhAbordaje");

			for (String token : List.of(ana.token(), flota.conductor().token(), api.admin())) {
				assertThat(marca(folio, token, hash)).isEqualTo(hora);
				assertThat(marca(folio, token, otroHash)).isNull();
			}
		}

		@Test
		@DisplayName("repetir devuelve la misma hora y no cambia la marca guardada")
		void repetir() throws InterruptedException {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String rut = api.rutUnico();
			String folio = viajeA(ana, flota, rut, 10);
			String hash = api.pasajeroConRut(ana, rut);
			String primera = Api.exigir(abordar(flota, rut), "abordar", 200).texto("$.fhAbordaje");

			Thread.sleep(1_100);
			String segunda = Api.exigir(abordar(flota, rut), "abordar otra vez", 200).texto("$.fhAbordaje");

			assertThat(segunda).isEqualTo(primera);
			assertThat(marca(folio, api.admin(), hash)).isEqualTo(primera);
		}

		@Test
		@DisplayName("el RUT se acepta con o sin puntos y guion, y el código con espacios y en minúsculas")
		void formatosEquivalentes() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String rut = api.rutUnico();
			viajeA(ana, flota, rut, 10);
			String conPuntos = rut.substring(0, 2) + "." + rut.substring(2, 5) + "." + rut.substring(5);
			String codigo = api.codigoAbordaje(flota.patente()).toLowerCase();
			String conEspacios = codigo.substring(0, 4) + " " + codigo.substring(4, 8) + " " + codigo.substring(8);

			Api.exigir(api.abordar(flota.patente().toLowerCase(), conEspacios, "RUT", conPuntos), "abordar", 200);
		}

		@Test
		@DisplayName("con un pasaporte también")
		void pasaporte() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String folio = api.viaje(ana, LocalDateTime.now().plusDays(3));
			String pasaporte = "P" + (4_000_000 + Math.abs(folio.hashCode() % 900_000));
			String hash = Api.exigir(api.post("/pasajeros", ana.token(), """
					{"tipoDocumento":"PASAPORTE","dni":"%s","nombre":"Pasajero"}""".formatted(pasaporte)),
					"crear pasajero", 200, 201).texto("$.dniHash");
			api.agregarPasajeroOk(ana, folio, hash);
			api.asignarOk(folio, flota.conductor().correo(), flota.patente());
			fijarAgenda(folio, ahora().plusMinutes(20));

			Api.exigir(api.abordar(flota.patente(), api.codigoAbordaje(flota.patente()), "PASAPORTE", pasaporte),
					"abordar", 200);

			assertThat(marca(folio, api.admin(), hash)).isNotNull();
		}
	}

	@Nested
	@DisplayName("Viajes candidatos")
	class Candidatos {

		@Test
		@DisplayName("ASIGNADO se abre una hora antes y se cierra una hora después de la agenda")
		void ventana() {
			Cuenta ana = api.solicitante();
			for (long minutos : new long[] {59, -59}) {
				Flota flota = api.flota(4);
				String rut = api.rutUnico();
				viajeA(ana, flota, rut, minutos);
				Api.exigir(abordar(flota, rut), "abordar a " + minutos + " min", 200);
			}
			for (long minutos : new long[] {61, -61}) {
				Flota flota = api.flota(4);
				String rut = api.rutUnico();
				String folio = viajeA(ana, flota, rut, minutos);

				assertNoEncontrado(abordar(flota, rut));

				assertThat(marca(folio, api.admin(), api.pasajeroConRut(ana, rut))).isNull();
			}
		}

		@Test
		@DisplayName("EN_CURSO es candidato aunque su agenda esté lejos")
		void enCurso() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String rut = api.rutUnico();
			String folio = viajeA(ana, flota, rut, 30);
			api.cambiarEstadoOk(folio, flota.conductor(), "EN_CURSO");
			fijarAgenda(folio, ahora().minusHours(6));

			Api.exigir(abordar(flota, rut), "abordar", 200);
		}

		@Test
		@DisplayName("un viaje cancelado o finalizado ya no es candidato")
		void cerrados() {
			Cuenta ana = api.solicitante();
			Flota cancelada = api.flota(4);
			String rutCancelado = api.rutUnico();
			String viajeCancelado = viajeA(ana, cancelada, rutCancelado, 20);
			api.cancelarOk(viajeCancelado, ana);
			Flota finalizada = api.flota(4);
			String rutFinalizado = api.rutUnico();
			String viajeFinalizado = viajeA(ana, finalizada, rutFinalizado, 20);
			api.cambiarEstadoOk(viajeFinalizado, finalizada.conductor(), "EN_CURSO");
			api.cambiarEstadoOk(viajeFinalizado, finalizada.conductor(), "FINALIZADO");

			assertNoEncontrado(abordar(cancelada, rutCancelado));
			assertNoEncontrado(abordar(finalizada, rutFinalizado));
		}

		@Test
		@DisplayName("un viaje de otro vehículo no cuenta")
		void otroVehiculo() {
			Cuenta ana = api.solicitante();
			Flota donde = api.flota(4);
			Flota otra = api.flota(4);
			String rut = api.rutUnico();
			viajeA(ana, donde, rut, 10);

			assertNoEncontrado(abordar(otra, rut));
		}

		@Test
		@DisplayName("si el documento calza en dos viajes del vehículo, gana el de agenda más cercana a ahora")
		void masCercano() {
			Cuenta ana = api.solicitante();
			for (long[] agendas : new long[][] {{-20, 50}, {-50, 20}}) {
				Flota flota = api.flota(4);
				String rut = api.rutUnico();
				String hash = api.pasajeroConRut(ana, rut);
				String primero = viajeA(ana, flota, rut, agendas[0]);
				String segundo = viajeA(ana, flota, rut, agendas[1]);

				Api.exigir(abordar(flota, rut), "abordar", 200);

				boolean ganaElPrimero = Math.abs(agendas[0]) < Math.abs(agendas[1]);
				assertThat(marca(primero, api.admin(), hash) != null).isEqualTo(ganaElPrimero);
				assertThat(marca(segundo, api.admin(), hash) != null).isEqualTo(!ganaElPrimero);
			}
		}
	}

	@Nested
	@DisplayName("El mismo 404 (RNF08)")
	class Reserva {

		@Test
		@DisplayName("patente inexistente, código ajeno y documento que no viaja responden igual")
		void mismoTexto() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String rut = api.rutUnico();
			viajeA(ana, flota, rut, 10);
			String codigo = api.codigoAbordaje(flota.patente());

			Resp patenteInexistente = api.abordar("ZZZZ99", codigo, "RUT", rut);
			Resp codigoAjeno = api.abordar(flota.patente(), api.codigoAbordaje(otra.patente()), "RUT", rut);
			Resp codigoAdivinado = api.abordar(flota.patente(), "0000000000000000", "RUT", rut);
			Resp documentoAjeno = api.abordar(flota.patente(), codigo, "RUT", api.rutUnico());

			for (Resp resp : List.of(patenteInexistente, codigoAjeno, codigoAdivinado, documentoAjeno)) {
				assertNoEncontrado(resp);
			}
		}

		@Test
		@DisplayName("con un código ajeno no marca nada aunque el documento viaje en ese vehículo")
		void codigoAjenoNoMarca() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			Flota otra = api.flota(4);
			String rut = api.rutUnico();
			String folio = viajeA(ana, flota, rut, 10);

			assertNoEncontrado(api.abordar(flota.patente(), api.codigoAbordaje(otra.patente()), "RUT", rut));

			assertThat(marca(folio, api.admin(), api.pasajeroConRut(ana, rut))).isNull();
		}
	}

	@Nested
	@DisplayName("Validación de la entrada (400 antes que el 404)")
	class Entrada {

		@Test
		@DisplayName("tipo, RUT, patente, código y cuerpo mal formados responden 400")
		void cuatrocientos() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			String rut = api.rutUnico();
			viajeA(ana, flota, rut, 10);
			String codigo = api.codigoAbordaje(flota.patente());
			String malRut = rut.substring(0, rut.length() - 1) + (rut.endsWith("0") ? "1" : "0");

			assertThat(api.abordar(flota.patente(), codigo, "DNI", rut).status()).isEqualTo(400);
			assertThat(api.abordar(flota.patente(), codigo, "RUT", malRut).status()).isEqualTo(400);
			assertThat(api.abordar(flota.patente(), codigo, "PASAPORTE", "AB1").status()).isEqualTo(400);
			assertThat(api.abordar("AB12", codigo, "RUT", rut).status()).isEqualTo(400);
			assertThat(api.abordar(flota.patente(), "XYZ", "RUT", rut).status()).isEqualTo(400);
			assertThat(api.abordar(flota.patente(), codigo, "RUT", "").status()).isEqualTo(400);
			assertThat(api.post("/abordajes", null, "{}").status()).isEqualTo(400);
			assertThat(api.post("/abordajes", null, null).status()).isEqualTo(400);
		}

		@Test
		@DisplayName("un RUT inválido responde 400 aunque el vehículo y el código no existan")
		void validacionAntesQueBusqueda() {
			assertThat(api.abordar("ZZZZ99", "0000000000000000", "RUT", "11111111-2").status()).isEqualTo(400);
		}

		@Test
		@DisplayName("solo existe el POST")
		void soloPost() {
			assertThat(api.get("/abordajes", null).status()).isIn(401, 403, 404, 405);
			assertThat(api.get("/abordajes", api.admin()).status()).isIn(403, 404, 405);
		}
	}
}
