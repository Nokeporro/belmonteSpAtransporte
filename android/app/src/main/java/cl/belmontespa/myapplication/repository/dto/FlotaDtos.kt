package cl.belmontespa.myapplication.repository.dto

import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ConductorRequest(val nombre: String, val correo: String, val telefono: String?, val password: String?)

@Serializable
data class ConductorResponse(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    @Serializable(with = SerializadorFecha::class) val fechaRegistro: LocalDateTime,
    val activo: Boolean,
    val estadoConductor: String,
    val viajesAsignados: Long,
    val folioEnCurso: String?,
)

@Serializable
data class ActivoRequest(val activo: Boolean)

@Serializable
data class MovilRequest(val patente: String, val capacidad: Int, val gpsSerial: Long?)

@Serializable
data class MovilResponse(
    val patente: String,
    val capacidad: Int,
    val gpsSerial: Long?,
    val activo: Boolean,
    val estadoMovil: String,
    val codigoAbordaje: String,
)

@Serializable
data class HabilitacionRequest(val patente: String, val correoConductor: String)

@Serializable
data class HabilitacionResponse(
    val patente: String,
    val correoConductor: String,
    val nombreConductor: String,
    val capacidad: Int,
    val estadoMovil: String,
    val activoMovil: Boolean,
    val estadoConductor: String,
    val activoConductor: Boolean,
)

fun ConductorResponse.aModelo() = Conductor(
    nombre, correo, telefono, fechaRegistro, activo, EstadoConductor.valueOf(estadoConductor), viajesAsignados.toInt(), folioEnCurso,
)

fun MovilResponse.aModelo() = Movil(patente, capacidad, gpsSerial, activo, EstadoMovil.valueOf(estadoMovil), codigoAbordaje)

fun HabilitacionResponse.aModelo() = Habilitacion(
    patente,
    capacidad,
    EstadoMovil.valueOf(estadoMovil),
    activoMovil,
    correoConductor,
    nombreConductor,
    EstadoConductor.valueOf(estadoConductor),
    activoConductor,
)
