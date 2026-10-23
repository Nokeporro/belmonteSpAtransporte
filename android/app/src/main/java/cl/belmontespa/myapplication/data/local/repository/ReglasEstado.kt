package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.ventanaDeInicio
import java.time.LocalDateTime

/**
 * V3 y V4 (README, Viaje): el mensaje del primer conflicto de pasar un viaje a `EN_CURSO` o `FINALIZADO`, o
 * `null` si se puede. Es la misma regla, con los mismos textos y el mismo orden, que aplica el backend.
 *
 * - V3 `ASIGNADO → EN_CURSO`: dentro de `fhAgenda ± 1 h` (extremos incluidos), y ni el conductor ni el vehículo
 *   con otro viaje `EN_CURSO`.
 * - V4 `EN_CURSO → FINALIZADO`: sin precondiciones.
 */
internal fun conflictoDeEstado(
    actual: EstadoViaje,
    nuevo: EstadoViaje,
    fhAgenda: LocalDateTime,
    ahora: LocalDateTime,
    conductorConOtroEnCurso: Boolean,
    movilConOtroEnCurso: Boolean,
): String? {
    val inicia = actual == EstadoViaje.ASIGNADO && nuevo == EstadoViaje.EN_CURSO
    val finaliza = actual == EstadoViaje.EN_CURSO && nuevo == EstadoViaje.FINALIZADO
    if (!inicia && !finaliza) return "No se puede pasar de $actual a $nuevo"
    if (finaliza) return null
    if (ventanaDeInicio(fhAgenda, ahora) != Inicio.HABILITADO) return "El viaje está fuera de la ventana de inicio"
    if (conductorConOtroEnCurso) return "El conductor ya tiene un viaje en curso"
    if (movilConOtroEnCurso) return "El móvil ya tiene un viaje en curso"
    return null
}
