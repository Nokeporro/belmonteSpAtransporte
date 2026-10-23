package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Pagos: registro (CU-11) y cambio de estado (CU-12)")
class PagoIT extends IntegrationTest {

	@Nested
	@DisplayName("Registrar un pago (RF18)")
	class Registro {

		@Test
		@DisplayName("el pago nace PENDIENTE con hash de 32 caracteres y queda ligado al viaje")
		void nace() {
			String folio = api.viaje(api.solicitante());
			Resp pago = Api.exigir(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", 45_000), "registrar pago", 201);
			assertThat(pago.texto("$.hash")).hasSize(32);
			assertThat(pago.texto("$.estadoPago")).isEqualTo("PENDIENTE");
			assertThat(pago.entero("$.monto")).isEqualTo(45_000);
			assertThat(pago.entero("$.codSbif")).isEqualTo(12);
			assertThat(pago.texto("$.medioPago")).isEqualTo("TRANSFERENCIA");
			Resp viaje = Api.exigir(api.verViaje(folio), "consultar viaje", 200);
			assertThat(viaje.texto("$.hashPago")).isEqualTo(pago.texto("$.hash"));
			assertThat(viaje.texto("$.estadoPago")).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("se puede registrar con el viaje PENDIENTE, ASIGNADO o FINALIZADO")
		void enCualquierEstadoPrevioACancelado() {
			Cuenta solicitante = api.solicitante();
			Flota flotaFinalizada = api.flota(4);
			String pendiente = api.viaje(solicitante);
			String asignado = api.viajeAsignado(solicitante, api.flota(4));
			String finalizado = api.viajeAsignado(solicitante, flotaFinalizada);
			api.iniciarOk(finalizado, flotaFinalizada.conductor());
			api.cambiarEstadoOk(finalizado, flotaFinalizada.conductor(), "FINALIZADO");
			assertThat(api.estadoViaje(finalizado)).isEqualTo("FINALIZADO");
			for (String folio : List.of(pendiente, asignado, finalizado)) {
				assertThat(api.pagarOk(folio)).hasSize(32);
			}
		}

		@Test
		@DisplayName("un segundo pago al mismo viaje responde 409")
		void segundoPago() {
			String folio = api.viaje(api.solicitante());
			api.pagarOk(folio);
			assertThat(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", 1_000).status()).isEqualTo(409);
		}

		@Test
		@DisplayName("un viaje cancelado responde 409 y uno inexistente, 404")
		void viajeNoPagable() {
			Cuenta solicitante = api.solicitante();
			String cancelado = api.viaje(solicitante);
			api.cancelarOk(cancelado, solicitante);
			assertThat(api.pagar(api.admin(), cancelado, 12, "TRANSFERENCIA", 1_000).status()).isEqualTo(409);
			assertThat(api.pagar(api.admin(), "TR-9999999", 12, "TRANSFERENCIA", 1_000).status()).isEqualTo(404);
			// Un folio mal formado en el cuerpo es una regla de validación: 400.
			assertThat(api.pagar(api.admin(), "NOEXISTE", 12, "TRANSFERENCIA", 1_000).status()).isEqualTo(400);
		}

		@Test
		@DisplayName("rechaza banco o medio de pago inexistente y montos no positivos con 400")
		void datosInvalidos() {
			String folio = api.viaje(api.solicitante());
			assertThat(api.pagar(api.admin(), folio, 999, "TRANSFERENCIA", 1_000).status()).isEqualTo(400);
			assertThat(api.pagar(api.admin(), folio, 12, "BITCOIN", 1_000).status()).isEqualTo(400);
			assertThat(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", 0).status()).isEqualTo(400);
			assertThat(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", -5).status()).isEqualTo(400);
			assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("solo el administrador registra pagos")
		void soloAdministrador() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			assertThat(api.pagar(solicitante.token(), folio, 12, "TRANSFERENCIA", 1_000).status()).isEqualTo(403);
			assertThat(api.pagar(api.conductor().token(), folio, 12, "TRANSFERENCIA", 1_000).status())
					.isEqualTo(403);
		}
	}

	@Nested
	@DisplayName("Cambiar el estado de un pago (RF19)")
	class Estados {

		@Test
		@DisplayName("PENDIENTE → PAGADO, y luego ya no se puede cambiar")
		void pagado() {
			String hash = api.pagarOk(api.viaje(api.solicitante()));
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "PAGADO").status()).isEqualTo(200);
			assertThat(api.estadoPago(hash)).isEqualTo("PAGADO");
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "PAGADO").status()).isEqualTo(409);
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "RECHAZADO").status()).isEqualTo(409);
		}

		@Test
		@DisplayName("PENDIENTE → RECHAZADO es definitivo")
		void rechazado() {
			String hash = api.pagarOk(api.viaje(api.solicitante()));
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "rechazado").status()).isEqualTo(200);
			assertThat(api.estadoPago(hash)).isEqualTo("RECHAZADO");
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "PAGADO").status()).isEqualTo(409);
		}

		@Test
		@DisplayName("ANULADO no se fija a mano, un pago inexistente responde 404 y solo el administrador cambia estados")
		void validaciones() {
			Cuenta solicitante = api.solicitante();
			String hash = api.pagarOk(api.viaje(solicitante));
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "ANULADO").status()).isEqualTo(400);
			assertThat(api.cambiarEstadoPago("NOEXISTE", api.admin(), "PAGADO").status()).isEqualTo(404);
			assertThat(api.cambiarEstadoPago(hash, solicitante.token(), "PAGADO").status()).isEqualTo(403);
			assertThat(api.estadoPago(hash)).isEqualTo("PENDIENTE");
		}

		@Test
		@DisplayName("el solicitante ve el estado de pago en el detalle de su viaje (RF20)")
		void estadoVisibleEnElViaje() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			String hash = api.pagarOk(folio);
			api.cambiarEstadoPago(hash, api.admin(), "PAGADO");
			Resp viaje = Api.exigir(api.get("/viajes/" + folio, solicitante.token()), "ver viaje", 200);
			assertThat(viaje.texto("$.estadoPago")).isEqualTo("PAGADO");
		}
	}

	@Nested
	@DisplayName("Al cancelar el viaje (CU-08)")
	class Cancelacion {

		@Test
		@DisplayName("un pago PENDIENTE o PAGADO queda ANULADO y uno RECHAZADO no cambia")
		void anulaSegunEstado() {
			Cuenta solicitante = api.solicitante();
			String conPendiente = api.viaje(solicitante);
			String conPagado = api.viaje(solicitante);
			String conRechazado = api.viaje(solicitante);
			String pendiente = api.pagarOk(conPendiente);
			String pagado = api.pagarOk(conPagado);
			String rechazado = api.pagarOk(conRechazado);
			api.cambiarEstadoPago(pagado, api.admin(), "PAGADO");
			api.cambiarEstadoPago(rechazado, api.admin(), "RECHAZADO");

			api.cancelarOk(conPendiente, solicitante);
			api.cancelarOk(conPagado, solicitante);
			api.cancelarOk(conRechazado, solicitante);

			assertThat(api.estadoPago(pendiente)).isEqualTo("ANULADO");
			assertThat(api.estadoPago(pagado)).isEqualTo("ANULADO");
			assertThat(api.estadoPago(rechazado)).isEqualTo("RECHAZADO");
		}

		@Test
		@DisplayName("un pago ANULADO no se puede volver a cambiar")
		void anuladoEsFinal() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			String hash = api.pagarOk(folio);
			api.cancelarOk(folio, solicitante);
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "PAGADO").status()).isEqualTo(409);
			assertThat(api.estadoPago(hash)).isEqualTo("ANULADO");
		}
	}

	@Nested
	@DisplayName("Alcance de la consulta (RF20)")
	class Consulta {

		@Test
		@DisplayName("el solicitante ve solo los pagos de sus viajes")
		void soloLosPropios() {
			Cuenta ana = api.solicitante();
			Cuenta beto = api.solicitante();
			String deAna = api.pagarOk(api.viaje(ana));
			String deBeto = api.pagarOk(api.viaje(beto));

			assertThat(api.get("/pagos/" + deAna, ana.token()).status()).isEqualTo(200);
			assertThat(api.get("/pagos/" + deBeto, ana.token()).status()).isEqualTo(403);
			Resp lista = Api.exigir(api.get("/pagos", ana.token()), "listar pagos", 200);
			assertThat(lista.<List<String>>json("$[*].hash")).contains(deAna).doesNotContain(deBeto);
		}

		@Test
		@DisplayName("el administrador ve todos y el conductor no tiene acceso")
		void administradorYConductor() {
			String hash = api.pagarOk(api.viaje(api.solicitante()));
			Resp todos = Api.exigir(api.get("/pagos", api.admin()), "listar pagos", 200);
			assertThat(todos.<List<String>>json("$[*].hash")).contains(hash);
			assertThat(api.get("/pagos", api.conductor().token()).status()).isEqualTo(403);
			assertThat(api.get("/pagos/NOEXISTE", api.admin()).status()).isEqualTo(404);
		}
	}
}
