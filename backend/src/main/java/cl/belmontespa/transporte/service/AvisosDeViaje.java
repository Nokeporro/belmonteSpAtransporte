package cl.belmontespa.transporte.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

final class AvisosDeViaje {

	record Aviso(String titulo, String mensaje, Map<String, String> datos) {}

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm");

	private AvisosDeViaje() {
	}

	static Aviso nuevoViaje(String folio, LocalDateTime fhAgenda) {
		return new Aviso("Nuevo viaje asignado", "Viaje " + folio + " para " + FECHA.format(fhAgenda), viaje(folio));
	}

	static Aviso viajeAsignado(String folio) {
		return new Aviso("Viaje asignado", "Tu viaje " + folio + " ya tiene conductor y vehículo", viaje(folio));
	}

	static Aviso viajeReasignado(String folio) {
		return new Aviso("Viaje reasignado", "El viaje " + folio + " fue asignado a otro conductor",
				Map.of("tipo", "AGENDA", "folio", folio));
	}

	static Aviso viajeRechazado(String folio) {
		return new Aviso("Viaje rechazado", "El viaje " + folio + " volvió a pendiente", viaje(folio));
	}

	static Aviso viajeSinConductor(String folio) {
		return new Aviso("Viaje sin conductor", "Tu viaje " + folio + " volvió a pendiente; se asignará otro conductor",
				viaje(folio));
	}

	private static Map<String, String> viaje(String folio) {
		return Map.of("tipo", "VIAJE", "folio", folio);
	}
}
