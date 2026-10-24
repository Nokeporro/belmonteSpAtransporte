package cl.belmontespa.transporte.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

// Push de V2, asignar y reasignar (README, Notificaciones): título, mensaje y `data`. `data.tipo` decide qué
// abre la app: VIAJE el detalle del viaje y AGENDA «Mis viajes» (al conductor reemplazado). El push lleva el
// folio y la fecha, sin nombres ni direcciones (RNF08).
final class AvisosDeViaje {

	record Aviso(String titulo, String mensaje, Map<String, String> datos) {}

	// `24-09-2026 · 14:30`
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm");

	private AvisosDeViaje() {
	}

	// Al conductor que queda con el viaje (nuevo, o el mismo con otro vehículo).
	static Aviso nuevoViaje(String folio, LocalDateTime fhAgenda) {
		return new Aviso("Nuevo viaje asignado", "Viaje " + folio + " para " + FECHA.format(fhAgenda), viaje(folio));
	}

	static Aviso viajeAsignado(String folio) {
		return new Aviso("Viaje asignado", "Tu viaje " + folio + " ya tiene conductor y vehículo", viaje(folio));
	}

	// Al conductor reemplazado: el viaje ya no es suyo, así que se abre su agenda y no el detalle.
	static Aviso viajeReasignado(String folio) {
		return new Aviso("Viaje reasignado", "El viaje " + folio + " fue asignado a otro conductor",
				Map.of("tipo", "AGENDA", "folio", folio));
	}

	private static Map<String, String> viaje(String folio) {
		return Map.of("tipo", "VIAJE", "folio", folio);
	}
}
