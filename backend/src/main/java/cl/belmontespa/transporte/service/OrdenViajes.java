package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.response.ViajeDTOResponse;
import cl.belmontespa.transporte.validation.Reglas;
import java.util.Comparator;
import java.util.Set;

// Orden de las listas de viajes (README, "Consultas → Orden"): por fhAgenda y, en el empate, por
// folio numérico en el mismo sentido. Ascendente (lo próximo primero) en la agenda y con estado
// PENDIENTE, ASIGNADO o EN_CURSO; descendente (lo reciente primero) en los demás casos.
final class OrdenViajes {

	static final Comparator<ViajeDTOResponse> PROXIMOS_PRIMERO = Comparator
			.comparing(ViajeDTOResponse::fhAgenda)
			.thenComparing(ViajeDTOResponse::folio, Reglas.FOLIO_POR_NUMERO);

	static final Comparator<ViajeDTOResponse> RECIENTES_PRIMERO = PROXIMOS_PRIMERO.reversed();

	private static final Set<String> DE_AGENDA = Set.of("PENDIENTE", "ASIGNADO", "EN_CURSO");

	private OrdenViajes() {
	}

	// El orden de `GET /viajes?estado=`.
	static Comparator<ViajeDTOResponse> paraEstado(String estado) {
		return DE_AGENDA.contains(estado) ? PROXIMOS_PRIMERO : RECIENTES_PRIMERO;
	}
}
