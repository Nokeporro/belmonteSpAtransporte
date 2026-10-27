package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

/** El conductor o el vehículo que se propone para el viaje, con lo que V2 necesita saber de él. */
internal data class ParticipanteAsignacion(
    val activo: Boolean,
    val disponible: Boolean,
    /** `fhAgenda` de sus otros viajes Asignados o En curso, sin contar el que se está asignando. */
    val agendaDeOtrosViajes: List<LocalDateTime>,
    /** El que ya tiene el viaje: no cambia, así que no se vuelve a validar. */
    val yaAsignado: Boolean,
)

/** Distancia mínima entre dos viajes de un mismo conductor o vehículo (README, V2). */
private const val MINUTOS_ENTRE_VIAJES = 120L

/**
 * V2 (README, Viaje): el mensaje del primer conflicto de asignar o reasignar un viaje, o `null` si se puede.
 * Es la misma regla, con los mismos textos y el mismo orden, que aplica el backend. Al reasignar solo se
 * valida lo que cambia; si no cambia ninguno, no hay nada que validar.
 */
internal fun conflictoDeAsignacion(
    estado: EstadoViaje,
    fhAgenda: LocalDateTime,
    cantidadPasajeros: Int,
    ahora: LocalDateTime,
    conductor: ParticipanteAsignacion,
    movil: ParticipanteAsignacion,
    capacidad: Int,
    habilitado: Boolean,
): String? {
    if (estado != EstadoViaje.PENDIENTE && estado != EstadoViaje.ASIGNADO) return "El viaje ya no se puede asignar"
    if (ahora.isAfter(fhAgenda.plusHours(1))) return "El viaje está vencido"
    if (cantidadPasajeros == 0) return "El viaje no tiene pasajeros"
    val cambiaConductor = !conductor.yaAsignado
    val cambiaMovil = !movil.yaAsignado
    if (!cambiaConductor && !cambiaMovil) return null
    if (cambiaConductor) {
        if (!conductor.activo) return "El conductor no está activo"
        if (!conductor.disponible) return "El conductor no está disponible"
    }
    if (cambiaMovil) {
        if (!movil.activo) return "El móvil no está activo"
        if (!movil.disponible) return "El móvil no está disponible"
    }
    if (!habilitado) return "El conductor no está habilitado para el móvil"
    if (cambiaMovil && cantidadPasajeros > capacidad) return "Los pasajeros superan la capacidad del móvil"
    if (cambiaConductor && conductor.agendaDeOtrosViajes.any { cercano(fhAgenda, it) }) {
        return "El conductor ya tiene un viaje a menos de 2 horas"
    }
    if (cambiaMovil && movil.agendaDeOtrosViajes.any { cercano(fhAgenda, it) }) {
        return "El móvil ya tiene un viaje a menos de 2 horas"
    }
    return null
}

// Extremos excluidos: a 120 minutos exactos se puede.
private fun cercano(fhAgenda: LocalDateTime, otra: LocalDateTime): Boolean =
    abs(Duration.between(fhAgenda, otra).toMinutes()) < MINUTOS_ENTRE_VIAJES
