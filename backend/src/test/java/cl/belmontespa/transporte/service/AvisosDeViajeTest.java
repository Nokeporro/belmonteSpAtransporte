package cl.belmontespa.transporte.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.service.AvisosDeViaje.Aviso;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AvisosDeViaje: los push de V2 y V8 según la tabla del README")
class AvisosDeViajeTest {

	@Test
	@DisplayName("al conductor: «Nuevo viaje asignado», con la fecha como 24-09-2026 · 14:30 y tipo VIAJE")
	void alConductor() {
		Aviso aviso = AvisosDeViaje.nuevoViaje("TR-1042", LocalDateTime.of(2026, 9, 24, 14, 30));

		assertThat(aviso.titulo()).isEqualTo("Nuevo viaje asignado");
		assertThat(aviso.mensaje()).isEqualTo("Viaje TR-1042 para 24-09-2026 · 14:30");
		assertThat(aviso.datos()).containsOnlyKeys("tipo", "folio")
				.containsEntry("tipo", "VIAJE").containsEntry("folio", "TR-1042");
	}

	@Test
	@DisplayName("la fecha lleva ceros a la izquierda y la hora en 24 horas")
	void formatoDeFecha() {
		assertThat(AvisosDeViaje.nuevoViaje("TR-1001", LocalDateTime.of(2027, 1, 5, 9, 5)).mensaje())
				.isEqualTo("Viaje TR-1001 para 05-01-2027 · 09:05");
		assertThat(AvisosDeViaje.nuevoViaje("TR-1001", LocalDateTime.of(2026, 12, 31, 0, 0)).mensaje())
				.isEqualTo("Viaje TR-1001 para 31-12-2026 · 00:00");
		assertThat(AvisosDeViaje.nuevoViaje("TR-1001", LocalDateTime.of(2026, 10, 28, 23, 59)).mensaje())
				.isEqualTo("Viaje TR-1001 para 28-10-2026 · 23:59");
	}

	@Test
	@DisplayName("al solicitante: «Viaje asignado» con tipo VIAJE")
	void alSolicitante() {
		Aviso aviso = AvisosDeViaje.viajeAsignado("TR-1042");

		assertThat(aviso.titulo()).isEqualTo("Viaje asignado");
		assertThat(aviso.mensaje()).isEqualTo("Tu viaje TR-1042 ya tiene conductor y vehículo");
		assertThat(aviso.datos()).containsOnlyKeys("tipo", "folio")
				.containsEntry("tipo", "VIAJE").containsEntry("folio", "TR-1042");
	}

	@Test
	@DisplayName("al conductor reemplazado: «Viaje reasignado» con tipo AGENDA, porque ya no puede abrir el viaje")
	void alReemplazado() {
		Aviso aviso = AvisosDeViaje.viajeReasignado("TR-1042");

		assertThat(aviso.titulo()).isEqualTo("Viaje reasignado");
		assertThat(aviso.mensaje()).isEqualTo("El viaje TR-1042 fue asignado a otro conductor");
		assertThat(aviso.datos()).containsOnlyKeys("tipo", "folio")
				.containsEntry("tipo", "AGENDA").containsEntry("folio", "TR-1042");
	}

	@Test
	@DisplayName("V8 a los Administradores: «Viaje rechazado» con tipo VIAJE")
	void alRechazarAvisaALosAdministradores() {
		Aviso aviso = AvisosDeViaje.viajeRechazado("TR-1042");

		assertThat(aviso.titulo()).isEqualTo("Viaje rechazado");
		assertThat(aviso.mensaje()).isEqualTo("El viaje TR-1042 volvió a pendiente");
		assertThat(aviso.datos()).containsOnlyKeys("tipo", "folio")
				.containsEntry("tipo", "VIAJE").containsEntry("folio", "TR-1042");
	}

	@Test
	@DisplayName("V8 al solicitante: «Viaje sin conductor» con tipo VIAJE")
	void alRechazarAvisaAlSolicitante() {
		Aviso aviso = AvisosDeViaje.viajeSinConductor("TR-1042");

		assertThat(aviso.titulo()).isEqualTo("Viaje sin conductor");
		assertThat(aviso.mensaje()).isEqualTo("Tu viaje TR-1042 volvió a pendiente; se asignará otro conductor");
		assertThat(aviso.datos()).containsOnlyKeys("tipo", "folio")
				.containsEntry("tipo", "VIAJE").containsEntry("folio", "TR-1042");
	}

	@Test
	@DisplayName("el push lleva el folio y la fecha, sin nombres ni direcciones (RNF08)")
	void sinDatosPersonales() {
		for (Aviso aviso : new Aviso[] {
				AvisosDeViaje.nuevoViaje("TR-1042", LocalDateTime.of(2026, 9, 24, 14, 30)),
				AvisosDeViaje.viajeAsignado("TR-1042"), AvisosDeViaje.viajeReasignado("TR-1042"),
					AvisosDeViaje.viajeRechazado("TR-1042"), AvisosDeViaje.viajeSinConductor("TR-1042") }) {
			assertThat(aviso.datos().values()).allMatch(valor -> valor.matches("VIAJE|AGENDA|TR-1042"));
		}
	}
}
