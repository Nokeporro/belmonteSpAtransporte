package cl.belmontespa.transporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.belmontespa.transporte.model.EstadoPago;
import cl.belmontespa.transporte.model.Pago;
import cl.belmontespa.transporte.repository.BancoRepository;
import cl.belmontespa.transporte.repository.EstadoPagoRepository;
import cl.belmontespa.transporte.repository.MedioPagoRepository;
import cl.belmontespa.transporte.repository.PagoRepository;
import cl.belmontespa.transporte.repository.TipoReciboRepository;
import cl.belmontespa.transporte.repository.ViajeRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("PagoService: efecto de la cancelación del Administrador (V7, P4 y P5)")
class PagoServiceCancelacionTest {

	private PagoService servicio;

	@BeforeEach
	void preparar() {
		EstadoPagoRepository estados = mock(EstadoPagoRepository.class);
		when(estados.findByEstado(anyString())).thenAnswer(invocacion -> Optional.of(estado(invocacion.getArgument(0))));
		servicio = new PagoService(mock(PagoRepository.class), mock(ViajeRepository.class), estados,
				mock(BancoRepository.class), mock(MedioPagoRepository.class), mock(TipoReciboRepository.class),
				mock(SolicitanteService.class), Clock.systemUTC());
	}

	private static EstadoPago estado(String nombre) {
		EstadoPago estado = new EstadoPago();
		estado.setEstado(nombre);
		return estado;
	}

	private static Pago pago(String estado, int monto) {
		Pago pago = new Pago();
		pago.setEstadoPago(estado(estado));
		pago.setMonto(monto);
		return pago;
	}

	private static void assert400(Runnable accion, String mensaje) {
		assertThatThrownBy(accion::run).isInstanceOfSatisfying(ResponseStatusException.class, e -> {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(e.getReason()).isEqualTo(mensaje);
		});
	}

	@Nested
	@DisplayName("cuánto se reembolsa")
	class Reembolso {

		@Test
		@DisplayName("con el viaje al día se reembolsa el total y se ignora el monto enviado")
		void alDia() {
			Pago pagado = pago("PAGADO", 52_000);

			assertThat(servicio.reembolsoDelAdministrador(pagado, false, null)).isEqualTo(52_000);
			assertThat(servicio.reembolsoDelAdministrador(pagado, false, 1)).isEqualTo(52_000);
			assertThat(servicio.reembolsoDelAdministrador(pagado, false, 999_999)).isEqualTo(52_000);
		}

		@Test
		@DisplayName("vencido y PAGADO, el Administrador fija el monto, de 0 al total")
		void vencidoFijado() {
			Pago pagado = pago("PAGADO", 52_000);

			assertThat(servicio.reembolsoDelAdministrador(pagado, true, 0)).isZero();
			assertThat(servicio.reembolsoDelAdministrador(pagado, true, 20_000)).isEqualTo(20_000);
			assertThat(servicio.reembolsoDelAdministrador(pagado, true, 52_000)).isEqualTo(52_000);
		}

		@Test
		@DisplayName("vencido y PAGADO, el monto es obligatorio y no puede salirse del rango")
		void vencidoInvalido() {
			Pago pagado = pago("PAGADO", 52_000);

			assert400(() -> servicio.reembolsoDelAdministrador(pagado, true, null), "montoReembolso: Es obligatorio.");
			assert400(() -> servicio.reembolsoDelAdministrador(pagado, true, -1),
					"montoReembolso: Debe estar entre 0 y 52000.");
			assert400(() -> servicio.reembolsoDelAdministrador(pagado, true, 52_001),
					"montoReembolso: Debe estar entre 0 y 52000.");
		}

		@Test
		@DisplayName("vencido pero sin pago PAGADO no se pide monto")
		void vencidoSinPagado() {
			assertThat(servicio.reembolsoDelAdministrador(pago("PENDIENTE", 30_000), true, null)).isEqualTo(30_000);
			assertThat(servicio.reembolsoDelAdministrador(pago("RECHAZADO", 30_000), true, 5)).isEqualTo(30_000);
		}
	}

	@Nested
	@DisplayName("qué le pasa al pago")
	class Efecto {

		@Test
		@DisplayName("PAGADO con reembolso pasa a REEMBOLSADO y guarda el monto")
		void reembolsado() {
			Pago pagado = pago("PAGADO", 52_000);

			servicio.cancelarPorAdministrador(pagado, 20_000);

			assertThat(pagado.getEstadoPago().getEstado()).isEqualTo("REEMBOLSADO");
			assertThat(pagado.getMontoReembolsado()).isEqualTo(20_000);
			assertThat(pagado.getMonto()).isEqualTo(52_000);
		}

		@Test
		@DisplayName("PAGADO con reembolso 0 sigue PAGADO, sin monto reembolsado")
		void conCeroSigue() {
			Pago pagado = pago("PAGADO", 52_000);

			servicio.cancelarPorAdministrador(pagado, 0);

			assertThat(pagado.getEstadoPago().getEstado()).isEqualTo("PAGADO");
			assertThat(pagado.getMontoReembolsado()).isNull();
		}

		@Test
		@DisplayName("PENDIENTE pasa a ANULADO sin reembolso")
		void anulado() {
			Pago pendiente = pago("PENDIENTE", 52_000);

			servicio.cancelarPorAdministrador(pendiente, 52_000);

			assertThat(pendiente.getEstadoPago().getEstado()).isEqualTo("ANULADO");
			assertThat(pendiente.getMontoReembolsado()).isNull();
		}

		@Test
		@DisplayName("los pagos terminales se conservan")
		void terminales() {
			for (String terminal : new String[] { "RECHAZADO", "ANULADO", "REEMBOLSADO" }) {
				Pago pago = pago(terminal, 52_000);
				pago.setMontoReembolsado(terminal.equals("REEMBOLSADO") ? 7_000 : null);

				servicio.cancelarPorAdministrador(pago, 52_000);

				assertThat(pago.getEstadoPago().getEstado()).isEqualTo(terminal);
				assertThat(pago.getMontoReembolsado()).isEqualTo(terminal.equals("REEMBOLSADO") ? 7_000 : null);
			}
		}
	}
}
