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
		@DisplayName("PENDIENTE → PAGADO no admite repetir ni rechazar")
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
		@DisplayName("los destinos automáticos no se fijan a mano, un pago inexistente responde 404 y solo el administrador cambia estados")
		void validaciones() {
			Cuenta solicitante = api.solicitante();
			String hash = api.pagarOk(api.viaje(solicitante));
			assertThat(api.cambiarEstadoPago(hash, api.admin(), "REEMBOLSADO").status()).isEqualTo(400);
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
			assertThat(api.estadoPago(pagado)).isEqualTo("REEMBOLSADO");
			assertThat(api.get("/pagos/" + pagado, solicitante.token()).entero("$.montoReembolsado")).isEqualTo(25_000);
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
	@DisplayName("Al cancelar el Administrador (CU-15, V7, P4 y P5)")
	class CancelacionDelAdministrador {

		private String pagado(String folio, int monto) {
			String hash = Api.exigir(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", monto), "registrar pago", 201)
					.texto("$.hash");
			Api.exigir(api.cambiarEstadoPago(hash, api.admin(), "PAGADO"), "marcar pagado", 200);
			return hash;
		}

		private Resp pago(String hash) {
			return Api.exigir(api.get("/pagos/" + hash, api.admin()), "consultar pago", 200);
		}

		@Test
		@DisplayName("con el viaje al día reembolsa el 100 % e ignora el monto que llegue")
		void reembolsaTodo() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			String hash = pagado(folio, 50_001);
			String fechaPago = pago(hash).texto("$.fechaPago");

			Resp viaje = Api.exigir(api.cancelarComoAdmin(folio, "Conductor sin disponibilidad", 1),
					"cancelar como Administrador", 200);

			assertThat(viaje.texto("$.estado")).isEqualTo("CANCELADO");
			assertThat(viaje.texto("$.estadoPago")).isEqualTo("REEMBOLSADO");
			Resp reembolso = pago(hash);
			assertThat(reembolso.texto("$.estadoPago")).isEqualTo("REEMBOLSADO");
			assertThat(reembolso.entero("$.montoReembolsado")).isEqualTo(50_001);
			assertThat(reembolso.entero("$.monto")).isEqualTo(50_001);
			assertThat(reembolso.texto("$.fechaPago")).isEqualTo(fechaPago);
			assertThat(api.get("/pagos/" + hash, solicitante.token()).entero("$.montoReembolsado")).isEqualTo(50_001);
		}

		@Test
		@DisplayName("vencido y PAGADO exige el monto, de 0 al total, y un 400 no cambia nada")
		void vencidoExigeElMonto() {
			String folio = api.viaje(api.solicitante());
			String hash = pagado(folio, 50_000);
			api.vencer(folio);

			Resp sinMonto = api.cancelarComoAdmin(folio, "Nadie lo atendió", null);
			assertThat(sinMonto.status()).isEqualTo(400);
			assertThat(sinMonto.texto("$.message")).isEqualTo("montoReembolso: Es obligatorio.");
			Resp demasiado = api.cancelarComoAdmin(folio, "Nadie lo atendió", 50_001);
			assertThat(demasiado.status()).isEqualTo(400);
			assertThat(demasiado.texto("$.message")).isEqualTo("montoReembolso: Debe estar entre 0 y 50000.");
			assertThat(api.cancelarComoAdmin(folio, "Nadie lo atendió", -1).status()).isEqualTo(400);

			assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
			assertThat(pago(hash).texto("$.estadoPago")).isEqualTo("PAGADO");
			assertThat(pago(hash).texto("$.montoReembolsado")).isNull();
		}

		@Test
		@DisplayName("vencido y PAGADO reembolsa el monto fijado o el total")
		void vencidoFijado() {
			Cuenta solicitante = api.solicitante();
			String parcial = api.viaje(solicitante);
			String hashParcial = pagado(parcial, 50_000);
			api.vencer(parcial);
			String total = api.viaje(solicitante);
			String hashTotal = pagado(total, 50_000);
			api.vencer(total);

			api.cancelarComoAdminOk(parcial, "Nadie lo atendió", 20_000);
			api.cancelarComoAdminOk(total, "Nadie lo atendió", 50_000);

			assertThat(pago(hashParcial).texto("$.estadoPago")).isEqualTo("REEMBOLSADO");
			assertThat(pago(hashParcial).entero("$.montoReembolsado")).isEqualTo(20_000);
			assertThat(pago(hashTotal).entero("$.montoReembolsado")).isEqualTo(50_000);
			assertThat(api.estadoViaje(parcial)).isEqualTo("CANCELADO");
		}

		@Test
		@DisplayName("vencido con monto 0 cancela el viaje y el pago sigue PAGADO, sin reembolso")
		void vencidoConCero() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			String hash = pagado(folio, 50_000);
			api.vencer(folio);

			Resp viaje = Api.exigir(api.cancelarComoAdmin(folio, "Sin reembolso", 0), "cancelar como Administrador", 200);

			assertThat(viaje.texto("$.estado")).isEqualTo("CANCELADO");
			assertThat(viaje.texto("$.estadoPago")).isEqualTo("PAGADO");
			assertThat(pago(hash).texto("$.estadoPago")).isEqualTo("PAGADO");
			assertThat(pago(hash).texto("$.montoReembolsado")).isNull();
		}

		@Test
		@DisplayName("un pago PENDIENTE queda ANULADO y vencido no pide monto; sin pago tampoco")
		void pendienteYSinPago() {
			Cuenta solicitante = api.solicitante();
			String conPendiente = api.viaje(solicitante);
			String hash = api.pagarOk(conPendiente);
			api.vencer(conPendiente);
			String sinPago = api.viaje(solicitante);
			api.vencer(sinPago);

			api.cancelarComoAdminOk(conPendiente, "Nadie lo atendió", null);
			api.cancelarComoAdminOk(sinPago, "Nadie lo atendió", null);

			assertThat(pago(hash).texto("$.estadoPago")).isEqualTo("ANULADO");
			assertThat(pago(hash).texto("$.montoReembolsado")).isNull();
			assertThat(api.estadoViaje(sinPago)).isEqualTo("CANCELADO");
		}

		@Test
		@DisplayName("un pago RECHAZADO no cambia y uno ya REEMBOLSADO queda final")
		void terminales() {
			Cuenta solicitante = api.solicitante();
			String folio = api.viaje(solicitante);
			String hash = api.pagarOk(folio);
			api.cambiarEstadoPago(hash, api.admin(), "RECHAZADO");

			api.cancelarComoAdminOk(folio, "Cambio de planes", null);

			assertThat(pago(hash).texto("$.estadoPago")).isEqualTo("RECHAZADO");
			String otro = api.viaje(solicitante);
			String reembolsado = pagado(otro, 40_000);
			api.cancelarComoAdminOk(otro, "Cambio de planes", null);
			assertThat(api.cambiarEstadoPago(reembolsado, api.admin(), "PAGADO").status()).isEqualTo(409);
			assertThat(pago(reembolsado).texto("$.estadoPago")).isEqualTo("REEMBOLSADO");
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
