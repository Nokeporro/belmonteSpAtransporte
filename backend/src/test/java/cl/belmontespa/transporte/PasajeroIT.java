package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pasajeros y su asociación a un viaje (CU-01)")
class PasajeroIT extends IntegrationTest {

	private int cantidadPasajeros(String folio) {
		return Api.exigir(api.verViaje(folio), "consultar viaje", 200).entero("$.cantidadPasajeros");
	}

	@Test
	@DisplayName("el DNI se guarda como hash de 43 caracteres, igual para el mismo DNI en otro formato")
	void hashDelDni() {
		Cuenta solicitante = api.solicitante();
		String dni = "%d".formatted(20_000_000 + System.nanoTime() % 1_000_000);
		String conFormato = dni.substring(0, 2) + "." + dni.substring(2, 5) + "." + dni.substring(5) + "-K";
		Resp a = Api.exigir(api.pasajero(solicitante.token(), conFormato, "Pasajero"), "crear pasajero", 200, 201);
		Resp b = Api.exigir(api.pasajero(solicitante.token(), dni + "-k", "Pasajero"), "mismo DNI sin formato", 200,
				201);
		assertThat(a.texto("$.dniHash")).hasSize(43).doesNotContain(dni);
		assertThat(b.texto("$.dniHash")).isEqualTo(a.texto("$.dniHash"));
	}

	@Test
	@DisplayName("rechaza DNI vacío con 400 y solo el solicitante crea pasajeros")
	void validacionYRol() {
		Cuenta solicitante = api.solicitante();
		assertThat(api.pasajero(solicitante.token(), "", "X").status()).isEqualTo(400);
		assertThat(api.pasajero(api.admin(), "1-9", "X").status()).isEqualTo(403);
		assertThat(api.pasajero(api.conductor().token(), "1-9", "X").status()).isEqualTo(403);
	}

	@Test
	@DisplayName("agregar pasajeros suma al contador del viaje y repetir uno no lo duplica")
	void contadorDePasajeros() {
		Cuenta solicitante = api.solicitante();
		String folio = api.viaje(solicitante);
		String uno = api.pasajero(solicitante);
		String dos = api.pasajero(solicitante);
		assertThat(cantidadPasajeros(folio)).isZero();
		api.agregarPasajeroOk(solicitante, folio, uno);
		api.agregarPasajeroOk(solicitante, folio, uno);
		assertThat(cantidadPasajeros(folio)).isEqualTo(1);
		api.agregarPasajeroOk(solicitante, folio, dos);
		assertThat(cantidadPasajeros(folio)).isEqualTo(2);
	}

	@Test
	@DisplayName("solo el solicitante dueño del viaje agrega pasajeros")
	void viajeAjeno() {
		Cuenta ana = api.solicitante();
		Cuenta beto = api.solicitante();
		String folio = api.viaje(ana);
		String pasajero = api.pasajero(ana);
		assertThat(api.agregarPasajero(beto.token(), folio, pasajero).status()).isEqualTo(403);
		assertThat(api.agregarPasajero(api.admin(), folio, pasajero).status()).isEqualTo(403);
		assertThat(cantidadPasajeros(folio)).isZero();
	}

	@Test
	@DisplayName("un viaje o un pasajero inexistente responde 404")
	void inexistentes() {
		Cuenta solicitante = api.solicitante();
		String folio = api.viaje(solicitante);
		String pasajero = api.pasajero(solicitante);
		assertThat(api.agregarPasajero(solicitante.token(), "NOEXISTE", pasajero).status()).isEqualTo(404);
		assertThat(api.agregarPasajero(solicitante.token(), folio, "hash-que-no-existe").status()).isEqualTo(404);
	}

	@Test
	@DisplayName("no se superan la capacidad del móvil asignado: el tercer pasajero en un móvil de 2 responde 409")
	void capacidadDelMovil() {
		Cuenta solicitante = api.solicitante();
		Flota flota = api.flota(2);
		String folio = api.viaje(solicitante);
		api.agregarPasajeroOk(solicitante, folio, api.pasajero(solicitante));
		api.agregarPasajeroOk(solicitante, folio, api.pasajero(solicitante));
		api.asignarOk(folio, flota.conductor().correo(), flota.patente());
		assertThat(api.agregarPasajero(solicitante.token(), folio, api.pasajero(solicitante)).status())
				.isEqualTo(409);
		assertThat(cantidadPasajeros(folio)).isEqualTo(2);
	}

	@Test
	@DisplayName("no se agregan pasajeros a un viaje FINALIZADO ni CANCELADO")
	void viajeCerrado() {
		Cuenta solicitante = api.solicitante();
		Flota flota = api.flota(4);
		String finalizado = api.viajeAsignado(solicitante, flota);
		api.cambiarEstadoOk(finalizado, flota.conductor(), "EN_CURSO");
		api.cambiarEstadoOk(finalizado, flota.conductor(), "FINALIZADO");
		String cancelado = api.viaje(solicitante);
		api.cancelarOk(cancelado, solicitante);
		String pasajero = api.pasajero(solicitante);
		assertThat(api.agregarPasajero(solicitante.token(), finalizado, pasajero).status()).isEqualTo(409);
		assertThat(api.agregarPasajero(solicitante.token(), cancelado, pasajero).status()).isEqualTo(409);
	}

	@Test
	@DisplayName("los pasajeros del viaje aparecen en el detalle")
	void detalleDelViaje() {
		Cuenta solicitante = api.solicitante();
		String folio = api.viaje(solicitante);
		String pasajero = api.pasajero(solicitante);
		api.agregarPasajeroOk(solicitante, folio, pasajero);
		Resp viaje = Api.exigir(api.get("/viajes/" + folio, solicitante.token()), "ver viaje", 200);
		assertThat(viaje.<java.util.List<?>>json("$.pasajeros")).hasSize(1);
		assertThat(api.get("/pasajeros-viaje/%s/%s".formatted(folio, pasajero), api.admin()).status())
				.isEqualTo(200);
	}
}
