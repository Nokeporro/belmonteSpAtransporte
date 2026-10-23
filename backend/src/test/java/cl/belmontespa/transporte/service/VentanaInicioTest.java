package cl.belmontespa.transporte.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Ventana de inicio de V3 (README, Máquinas de estado)")
class VentanaInicioTest {

	private static final LocalDateTime AGENDA = LocalDateTime.of(2026, 10, 28, 10, 0);

	@Test
	@DisplayName("los dos bordes, una hora antes y una hora después, están dentro")
	void bordesIncluidos() {
		assertThat(ViajeService.dentroDeLaVentanaDeInicio(AGENDA, AGENDA.minusHours(1))).isTrue();
		assertThat(ViajeService.dentroDeLaVentanaDeInicio(AGENDA, AGENDA.plusHours(1))).isTrue();
	}

	@Test
	@DisplayName("un segundo más allá de cualquiera de los bordes está fuera")
	void fueraPorUnSegundo() {
		assertThat(ViajeService.dentroDeLaVentanaDeInicio(AGENDA, AGENDA.minusHours(1).minusSeconds(1))).isFalse();
		assertThat(ViajeService.dentroDeLaVentanaDeInicio(AGENDA, AGENDA.plusHours(1).plusSeconds(1))).isFalse();
	}

	@Test
	@DisplayName("la hora agendada y los extremos de la ventana por dentro están dentro")
	void dentro() {
		assertThat(ViajeService.dentroDeLaVentanaDeInicio(AGENDA, AGENDA)).isTrue();
		assertThat(ViajeService.dentroDeLaVentanaDeInicio(AGENDA, AGENDA.minusHours(1).plusSeconds(1))).isTrue();
		assertThat(ViajeService.dentroDeLaVentanaDeInicio(AGENDA, AGENDA.plusHours(1).minusSeconds(1))).isTrue();
	}
}
