package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import cl.belmontespa.transporte.validation.Reglas;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Orden fijo de las listas (README, "Consultas → Orden"). El esquema lo comparten todas las clases de
 * integración, así que cada test crea sus propios registros y compara solo la posición relativa de
 * ellos dentro de la lista completa. Los nombres mezclan mayúsculas y tilde («Álvaro», «beto», «Zoe»)
 * para distinguir la collation utf8mb4_unicode_ci del orden binario, donde «Zoe» < «beto» < «Álvaro».
 */
@DisplayName("Orden fijo de las listas (README, Consultas)")
class OrdenIT extends IntegrationTest {

	private static final AtomicInteger CONTADOR = new AtomicInteger(100_000);

	private static String rut() {
		String cuerpo = "78%06d".formatted(CONTADOR.incrementAndGet());
		return cuerpo + "-" + Reglas.digitoVerificador(cuerpo);
	}

	/** Falla si alguno de los esperados falta o si no aparecen en el orden dado. */
	private static void assertEnOrden(List<String> lista, String... esperadosEnOrden) {
		List<Integer> posiciones = new ArrayList<>();
		for (String esperado : esperadosEnOrden) {
			posiciones.add(lista.indexOf(esperado));
		}
		assertThat(posiciones).as("posiciones de %s en %s", List.of(esperadosEnOrden), lista)
				.doesNotContain(-1).isSorted();
	}

	private List<String> campo(String ruta, String token, String path) {
		return Api.exigir(api.get(ruta, token), "listar " + ruta, 200).<List<String>>json(path);
	}

	@Nested
	@DisplayName("Cuentas, empresas y pasajeros")
	class Cuentas {

		@Test
		@DisplayName("conductores, solicitantes y empresas se ordenan por nombre y razón social, sin distinguir mayúsculas ni tildes")
		void porTexto() {
			// Por nombre: Álvaro, beto, Zoe. Por razón social: Álamo, beta, Zeta (otro orden).
			String zoe = "zoe-" + System.nanoTime() + "@test.cl";
			String beto = "beto-" + System.nanoTime() + "@test.cl";
			String alvaro = "alvaro-" + System.nanoTime() + "@test.cl";
			registrar(zoe, "Zoe", "Álamo SA");
			registrar(beto, "beto", "Zeta Ltda");
			registrar(alvaro, "Álvaro", "beta SpA");

			assertEnOrden(campo("/solicitantes", api.admin(), "$[*].correo"), alvaro, beto, zoe);
			assertEnOrden(campo("/empresas", api.admin(), "$[*].razonSocial"), "Álamo SA", "beta SpA", "Zeta Ltda");
		}

		private void registrar(String correo, String nombre, String razonSocial) {
			String cuerpo = api.cuerpoSolicitante(correo, rut(), razonSocial)
					.replace("\"nombre\":\"Solicitante\"", "\"nombre\":\"" + nombre + "\"");
			Api.exigir(api.post("/solicitantes", null, cuerpo), "registrar " + correo, 201);
		}

		@Test
		@DisplayName("los conductores se ordenan por nombre")
		void conductores() {
			String zoe = "czoe-" + System.nanoTime() + "@test.cl";
			String beto = "cbeto-" + System.nanoTime() + "@test.cl";
			String alvaro = "calvaro-" + System.nanoTime() + "@test.cl";
			for (String[] datos : new String[][] {{zoe, "Zoe"}, {beto, "beto"}, {alvaro, "Álvaro"}}) {
				String cuerpo = api.cuerpoConductor(datos[0]).replace("\"nombre\":\"Conductor\"",
						"\"nombre\":\"" + datos[1] + "\"");
				Api.exigir(api.post("/conductores", api.admin(), cuerpo), "crear conductor", 201);
			}
			assertEnOrden(campo("/conductores", api.admin(), "$[*].correo"), alvaro, beto, zoe);
		}

		@Test
		@DisplayName("los pasajeros se ordenan por nombre")
		void pasajeros() {
			Cuenta solicitante = api.solicitante();
			for (String nombre : new String[] {"Zoe Pasajera", "beto Pasajero", "Álvaro Pasajero"}) {
				String cuerpo = "%d".formatted(30_000_000 + CONTADOR.incrementAndGet());
				String dni = cuerpo + "-" + Reglas.digitoVerificador(cuerpo);
				Api.exigir(api.pasajero(solicitante.token(), dni, nombre), "crear pasajero " + nombre, 200, 201);
			}
			assertEnOrden(campo("/pasajeros", api.admin(), "$[*].nombre"),
					"Álvaro Pasajero", "beto Pasajero", "Zoe Pasajera");
		}
	}

	@Nested
	@DisplayName("Flota")
	class FlotaYHabilitaciones {

		@Test
		@DisplayName("los móviles se ordenan por patente, no por orden de alta")
		void moviles() {
			for (String patente : new String[] {"ZQAA03", "ZQAA01", "ZQAA02"}) {
				Api.exigir(api.post("/moviles", api.admin(), api.cuerpoMovil(patente, 2)), "crear móvil " + patente,
						201);
			}
			assertEnOrden(campo("/moviles", api.admin(), "$[*].patente"), "ZQAA01", "ZQAA02", "ZQAA03");
		}

		@Test
		@DisplayName("las habilitaciones se ordenan por patente y, dentro de ella, por nombre del conductor")
		void habilitaciones() {
			String zoe = conductorLlamado("Zoe");
			String alvaro = conductorLlamado("Álvaro");
			for (String patente : new String[] {"ZQBB02", "ZQBB01"}) {
				Api.exigir(api.post("/moviles", api.admin(), api.cuerpoMovil(patente, 2)), "crear móvil", 201);
			}
			// Se habilitan en un orden distinto al esperado.
			api.habilitarOk("ZQBB02", alvaro);
			api.habilitarOk("ZQBB01", zoe);
			api.habilitarOk("ZQBB02", zoe);
			api.habilitarOk("ZQBB01", alvaro);

			Resp lista = Api.exigir(api.get("/habilitaciones-movil-conductor", api.admin()), "listar", 200);
			List<String> patentes = lista.json("$[*].patente");
			List<String> nombres = lista.json("$[*].nombreConductor");
			List<String> propias = new ArrayList<>();
			for (int i = 0; i < patentes.size(); i++) {
				if (patentes.get(i).startsWith("ZQBB")) {
					propias.add(patentes.get(i) + " " + nombres.get(i));
				}
			}
			assertThat(propias).containsExactly("ZQBB01 Álvaro", "ZQBB01 Zoe", "ZQBB02 Álvaro", "ZQBB02 Zoe");
		}

		/** Crea un conductor con ese nombre y devuelve su correo. */
		private String conductorLlamado(String nombre) {
			String correo = "h" + nombre.toLowerCase().charAt(0) + "-" + System.nanoTime() + "@test.cl";
			String cuerpo = api.cuerpoConductor(correo).replace("\"nombre\":\"Conductor\"",
					"\"nombre\":\"" + nombre + "\"");
			Api.exigir(api.post("/conductores", api.admin(), cuerpo), "crear conductor", 201);
			return correo;
		}
	}

	@Nested
	@DisplayName("Pagos")
	class Pagos {

		@Test
		@DisplayName("los pagos van por folio descendente, tanto para el Administrador como para el Solicitante")
		void porFolioDescendente() {
			Cuenta solicitante = api.solicitante();
			String primero = api.viaje(solicitante);
			String segundo = api.viaje(solicitante);
			String tercero = api.viaje(solicitante);
			// Se pagan en un orden distinto al de los folios.
			String hashSegundo = api.pagarOk(segundo);
			String hashPrimero = api.pagarOk(primero);
			String hashTercero = api.pagarOk(tercero);

			assertEnOrden(campo("/pagos", api.admin(), "$[*].hash"), hashTercero, hashSegundo, hashPrimero);
			assertThat(campo("/pagos", solicitante.token(), "$[*].hash"))
					.containsExactly(hashTercero, hashSegundo, hashPrimero);
		}
	}

	@Nested
	@DisplayName("Viajes")
	class Viajes {

		private final LocalDateTime base = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0)
				.withNano(0);

		private String viajeEn(Cuenta solicitante, LocalDateTime fhAgenda) {
			return Api.exigir(api.post("/viajes", solicitante.token(), api.cuerpoViaje(fhAgenda, "Providencia")),
					"crear viaje", 201).texto("$.folio");
		}

		private List<String> folios(String ruta, String token) {
			return campo(ruta, token, "$[*].folio");
		}

		@Test
		@DisplayName("sin estado, lo reciente primero; con PENDIENTE, lo próximo primero")
		void porEstado() {
			Cuenta ana = api.solicitante();
			// Se crean en un orden de agenda que no es el de los folios: +2, +0 y +1 días.
			String lejos = viajeEn(ana, base.plusDays(2));
			String cerca = viajeEn(ana, base);
			String medio = viajeEn(ana, base.plusDays(1));

			assertThat(folios("/viajes", ana.token())).containsExactly(lejos, medio, cerca);
			assertEnOrden(folios("/viajes", api.admin()), lejos, medio, cerca);
			assertEnOrden(folios("/viajes?estado=PENDIENTE", api.admin()), cerca, medio, lejos);
		}

		@Test
		@DisplayName("CANCELADO va con lo reciente primero")
		void estadosDeHistorial() {
			Cuenta ana = api.solicitante();
			String lejos = viajeEn(ana, base.plusDays(2));
			String cerca = viajeEn(ana, base);
			String medio = viajeEn(ana, base.plusDays(1));
			for (String folio : List.of(cerca, lejos, medio)) {
				api.cancelarOk(folio, ana);
			}

			assertEnOrden(folios("/viajes?estado=CANCELADO", api.admin()), lejos, medio, cerca);
			assertThat(folios("/viajes?estado=PENDIENTE", api.admin())).doesNotContain(lejos, medio, cerca);
		}

		@Test
		@DisplayName("con la misma hora agendada, el empate se desata por folio en el mismo sentido")
		void empate() {
			Cuenta ana = api.solicitante();
			LocalDateTime mismaHora = base.plusDays(3);
			String uno = viajeEn(ana, mismaHora);
			String dos = viajeEn(ana, mismaHora);
			String tres = viajeEn(ana, mismaHora);

			assertThat(folios("/viajes", ana.token())).containsExactly(tres, dos, uno);
			assertEnOrden(folios("/viajes?estado=PENDIENTE", api.admin()), uno, dos, tres);
		}

		@Test
		@DisplayName("la agenda del Conductor va con lo próximo primero")
		void agendaDelConductor() {
			Cuenta ana = api.solicitante();
			Flota flota = api.flota(4);
			LocalDate dia = base.toLocalDate().plusDays(5);
			// El de la tarde se crea primero (folio menor) y se termina; el de la mañana queda asignado.
			String tarde = viajeEn(ana, dia.atTime(LocalTime.of(16, 0)));
			String manana = viajeEn(ana, dia.atTime(LocalTime.of(8, 0)));
			api.asignarOk(tarde, flota.conductor().correo(), flota.patente());
			api.cambiarEstadoOk(tarde, flota.conductor(), "EN_CURSO");
			api.cambiarEstadoOk(tarde, flota.conductor(), "FINALIZADO");
			api.asignarOk(manana, flota.conductor().correo(), flota.patente());

			assertThat(folios("/viajes?fecha=" + dia, flota.conductor().token())).containsExactly(manana, tarde);
		}
	}
}
