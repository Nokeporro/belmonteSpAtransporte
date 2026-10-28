package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.ventanaDeInicio
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

internal fun esCandidatoDeAbordaje(estado: EstadoViaje, fhAgenda: LocalDateTime, ahora: LocalDateTime): Boolean =
    estado == EstadoViaje.EN_CURSO ||
        estado == EstadoViaje.ASIGNADO && ventanaDeInicio(fhAgenda, ahora) == Inicio.HABILITADO

internal fun <T> ordenarPorCercania(viajes: List<T>, ahora: LocalDateTime, fhAgenda: (T) -> LocalDateTime): List<T> =
    viajes.sortedBy { abs(Duration.between(ahora, fhAgenda(it)).seconds) }
