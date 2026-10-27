package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.ventanaDeInicio
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Candidatos del abordaje (README, Abordaje): los viajes del vehículo `EN_CURSO`, o `ASIGNADO` dentro de la ventana de
 * V3 (`fhAgenda ± 1 h`, extremos incluidos). Es la misma regla que aplica el backend en `AbordajeService`.
 */
internal fun esCandidatoDeAbordaje(estado: EstadoViaje, fhAgenda: LocalDateTime, ahora: LocalDateTime): Boolean =
    estado == EstadoViaje.EN_CURSO ||
        estado == EstadoViaje.ASIGNADO && ventanaDeInicio(fhAgenda, ahora) == Inicio.HABILITADO

/** Si el documento calza en varios viajes, gana el de `fhAgenda` más cercana a ahora; a igual distancia, el que venía antes. */
internal fun <T> ordenarPorCercania(viajes: List<T>, ahora: LocalDateTime, fhAgenda: (T) -> LocalDateTime): List<T> =
    viajes.sortedBy { abs(Duration.between(ahora, fhAgenda(it)).seconds) }
