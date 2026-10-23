package cl.belmontespa.myapplication.model

import java.time.LocalDate
import java.time.LocalDateTime

/** Origen o destino, elegido desde una sugerencia de Google Places. */
data class Ubicacion(
    val direccion: String,
    val comuna: String,
    val region: String,
    val latitud: Double,
    val longitud: Double,
)

/** Pasajero dentro de un viaje; `fhAbordaje` en null significa Por abordar. */
data class PasajeroViaje(
    val dniHash: String,
    val nombre: String,
    val fhAbordaje: LocalDateTime?,
)

/** Viaje como lo entrega `ViajeDTOResponse`. */
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

/** Filtros combinables de `GET /viajes`; sin filtros, el Conductor ve su agenda de hoy. */
data class FiltroViajes(
    val estado: EstadoViaje? = null,
    val fecha: LocalDate? = null,
)

/** Pasajero que el Solicitante agrega; el documento nunca se guarda en claro. */
data class NuevoPasajero(
    val tipoDocumento: TipoDocumento,
    val dni: String,
    val nombre: String,
)

/** Datos de "Nueva solicitud" (README, Flujo de una solicitud). */
data class NuevaSolicitud(
    val fhAgenda: LocalDateTime,
    val origen: Ubicacion,
    val destino: Ubicacion,
    val numeroVuelo: String?,
    val pasajeros: List<NuevoPasajero>,
)
