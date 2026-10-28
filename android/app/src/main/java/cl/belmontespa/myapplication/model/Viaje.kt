package cl.belmontespa.myapplication.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Ubicacion(
    val direccion: String,
    val comuna: String,
    val region: String,
    val latitud: Double,
    val longitud: Double,
)

data class PasajeroViaje(
    val dniHash: String,
    val nombre: String,
    val fhAbordaje: LocalDateTime?,
)

data class Viaje(
    val folio: String,
    val estado: EstadoViaje,
    val correoSolicitante: String,
    val nombreSolicitante: String,
    val telefonoSolicitante: String?,
    val razonSocial: String,
    val origen: Ubicacion,
    val destino: Ubicacion,
    val fhCreacion: LocalDateTime,
    val fhAgenda: LocalDateTime,
    val fhAsignacion: LocalDateTime?,
    val fhInicio: LocalDateTime?,
    val fhFin: LocalDateTime?,
    val numeroVuelo: String?,
    val cantidadPasajeros: Int,
    val correoConductor: String?,
    val nombreConductor: String?,
    val patenteMovil: String?,
    val hashPago: String?,
    val estadoPago: EstadoPago?,
    val motivo: String?,
    val pasajeros: List<PasajeroViaje>,
)

data class FiltroViajes(
    val estado: EstadoViaje? = null,
    val fecha: LocalDate? = null,
)

data class NuevoPasajero(
    val tipoDocumento: TipoDocumento,
    val dni: String,
    val nombre: String,
)

data class NuevaSolicitud(
    val fhAgenda: LocalDateTime,
    val origen: Ubicacion,
    val destino: Ubicacion,
    val numeroVuelo: String?,
    val pasajeros: List<NuevoPasajero>,
)
