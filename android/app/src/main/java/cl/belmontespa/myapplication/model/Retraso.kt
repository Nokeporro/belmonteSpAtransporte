package cl.belmontespa.myapplication.model

import java.time.Clock
import java.time.LocalDateTime

enum class Retraso { ATRASADO, VENCIDO }

fun Viaje.retraso(clock: Clock): Retraso? {
    if (estado != EstadoViaje.PENDIENTE && estado != EstadoViaje.ASIGNADO) return null
    val ahora = LocalDateTime.now(clock)
    return when {
        ahora > fhAgenda.plusHours(1) -> Retraso.VENCIDO
        ahora > fhAgenda -> Retraso.ATRASADO
        else -> null
    }
}

enum class Inicio { HABILITADO, ANTES_DE_LA_VENTANA, VENCIDO, NO_APLICA }

fun ventanaDeInicio(fhAgenda: LocalDateTime, ahora: LocalDateTime): Inicio = when {
    ahora < fhAgenda.minusHours(1) -> Inicio.ANTES_DE_LA_VENTANA
    ahora > fhAgenda.plusHours(1) -> Inicio.VENCIDO
    else -> Inicio.HABILITADO
}

fun Viaje.ventanaDeInicio(clock: Clock): Inicio =
    if (estado == EstadoViaje.ASIGNADO) ventanaDeInicio(fhAgenda, LocalDateTime.now(clock)) else Inicio.NO_APLICA

fun Viaje.disponibleDesde(): LocalDateTime = fhAgenda.minusHours(1)

fun Viaje.asignable(clock: Clock): Boolean =
    (estado == EstadoViaje.PENDIENTE || estado == EstadoViaje.ASIGNADO) &&
        cantidadPasajeros > 0 &&
        retraso(clock) != Retraso.VENCIDO

fun Viaje.cancelablePorSolicitante(ahora: LocalDateTime): Boolean =
    estado in listOf(EstadoViaje.PENDIENTE, EstadoViaje.ASIGNADO) && ahora <= fhAgenda.plusHours(1)
