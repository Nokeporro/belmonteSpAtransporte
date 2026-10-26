package cl.belmontespa.myapplication.model

import java.time.Clock
import java.time.LocalDateTime

/**
 * Retraso de un viaje sin iniciar. Se calcula, no es un estado (README, Retraso): lo calcula la
 * app al mostrar, también sin conexión, con el `Clock` inyectado (hora de Chile).
 */
enum class Retraso { ATRASADO, VENCIDO }

/**
 * Atrasado si `fhAgenda < ahora ≤ fhAgenda + 1 h` (aún puede iniciarse) y Vencido si
 * `ahora > fhAgenda + 1 h`. Solo un viaje `PENDIENTE` o `ASIGNADO` puede llevar retraso. La
 * aritmética es civil (`plusHours`), como pide el README (Fechas).
 */
fun Viaje.retraso(clock: Clock): Retraso? {
    if (estado != EstadoViaje.PENDIENTE && estado != EstadoViaje.ASIGNADO) return null
    val ahora = LocalDateTime.now(clock)
    return when {
        ahora > fhAgenda.plusHours(1) -> Retraso.VENCIDO
        ahora > fhAgenda -> Retraso.ATRASADO
        else -> null
    }
}

/** Dónde está «Iniciar viaje» respecto de la ventana de V3 (`fhAgenda` ± 1 h). */
enum class Inicio { HABILITADO, ANTES_DE_LA_VENTANA, VENCIDO, NO_APLICA }

/**
 * Ventana de inicio (V3): `fhAgenda − 1 h ≤ ahora ≤ fhAgenda + 1 h`, con ambos extremos incluidos.
 * Devuelve `HABILITADO` dentro de la ventana y `ANTES_DE_LA_VENTANA` o `VENCIDO` fuera de ella.
 */
fun ventanaDeInicio(fhAgenda: LocalDateTime, ahora: LocalDateTime): Inicio = when {
    ahora < fhAgenda.minusHours(1) -> Inicio.ANTES_DE_LA_VENTANA
    ahora > fhAgenda.plusHours(1) -> Inicio.VENCIDO
    else -> Inicio.HABILITADO
}

/** Solo un viaje `ASIGNADO` puede iniciarse: en cualquier otro estado la ventana no aplica. */
fun Viaje.ventanaDeInicio(clock: Clock): Inicio =
    if (estado == EstadoViaje.ASIGNADO) ventanaDeInicio(fhAgenda, LocalDateTime.now(clock)) else Inicio.NO_APLICA

/** Desde cuándo puede iniciarse: el "Disponible desde las {hora}" del Conductor. */
fun Viaje.disponibleDesde(): LocalDateTime = fhAgenda.minusHours(1)

/**
 * "Asignar" (o "Reasignar") solo se ofrece a un viaje `PENDIENTE` o `ASIGNADO`, con al menos un
 * pasajero y que no esté vencido (README, Navegación y pantallas).
 */
fun Viaje.asignable(clock: Clock): Boolean =
    (estado == EstadoViaje.PENDIENTE || estado == EstadoViaje.ASIGNADO) &&
        cantidadPasajeros > 0 &&
        retraso(clock) != Retraso.VENCIDO

/** V5 incluye el instante exacto de agenda + 1 h; después requiere al Administrador. */
fun Viaje.cancelablePorSolicitante(ahora: LocalDateTime): Boolean =
    estado in listOf(EstadoViaje.PENDIENTE, EstadoViaje.ASIGNADO) && ahora <= fhAgenda.plusHours(1)
