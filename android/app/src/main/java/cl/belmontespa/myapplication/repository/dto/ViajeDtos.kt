package cl.belmontespa.myapplication.repository.dto

import cl.belmontespa.myapplication.model.PasajeroEntity
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.ViajeEntity
import cl.belmontespa.myapplication.model.ViajePasajeroEntity
import cl.belmontespa.myapplication.util.FechaIso
import java.time.LocalDateTime
import kotlin.math.round
import kotlinx.serialization.Serializable

@Serializable
data class UbicacionDto(
    val direccion: String,
    val comuna: String,
    val region: String,
    val latitud: Double,
    val longitud: Double,
)

@Serializable
data class ViajeRequest(
    @Serializable(with = SerializadorFecha::class) val fhAgenda: LocalDateTime,
    val origen: UbicacionDto,
    val destino: UbicacionDto,
    val numeroVuelo: String?,
)

@Serializable
data class AsignarRequest(val correoConductor: String, val patente: String)

@Serializable
data class EstadoViajeRequest(val estado: String)

@Serializable
data class CancelarRequest(val motivo: String, val montoReembolso: Int?)

@Serializable
data class RechazarRequest(val motivo: String)

@Serializable
data class PasajeroRequest(val tipoDocumento: String, val dni: String, val nombre: String)

@Serializable
data class PasajeroResponse(val dniHash: String, val nombre: String? = null)

@Serializable
data class PasajeroViajeRequest(val folio: String, val dniHash: String)

@Serializable
data class PasajeroViajeResponse(val folio: String, val dniHash: String)

@Serializable
data class PasajeroDeViajeDto(
    val dniHash: String,
    val nombre: String? = null,
    @Serializable(with = SerializadorFecha::class) val fhAbordaje: LocalDateTime? = null,
)

@Serializable
data class ViajeResponse(
    val folio: String,
    val estado: String,
    val correoSolicitante: String,
    val nombreSolicitante: String,
    val telefonoSolicitante: String?,
    val razonSocial: String,
    val origen: UbicacionDto,
    val destino: UbicacionDto,
    @Serializable(with = SerializadorFecha::class) val fhCreacion: LocalDateTime,
    @Serializable(with = SerializadorFecha::class) val fhAgenda: LocalDateTime,
    @Serializable(with = SerializadorFecha::class) val fhAsignacion: LocalDateTime?,
    @Serializable(with = SerializadorFecha::class) val fhInicio: LocalDateTime?,
    @Serializable(with = SerializadorFecha::class) val fhFin: LocalDateTime?,
    val numeroVuelo: String?,
    val cantidadPasajeros: Int,
    val correoConductor: String?,
    val nombreConductor: String?,
    val patenteMovil: String?,
    val hashPago: String?,
    val estadoPago: String?,
    val motivo: String?,
    val pasajeros: List<PasajeroDeViajeDto>,
)

@Serializable
data class AbordajeRequest(val patente: String, val codigoAbordaje: String, val tipoDocumento: String, val dni: String)

@Serializable
data class AbordajeResponse(@Serializable(with = SerializadorFecha::class) val fhAbordaje: LocalDateTime)

class ViajeGuardable(
    val viaje: ViajeEntity,
    val pasajeros: List<PasajeroEntity>,
    val detalles: List<ViajePasajeroEntity>,
)

fun Ubicacion.aDto() = UbicacionDto(direccion, comuna, region, seisDecimales(latitud), seisDecimales(longitud))

fun NuevoPasajero.aRequest() = PasajeroRequest(tipoDocumento.name, dni, nombre)

fun ViajeResponse.aGuardable(descargadoEn: Long, nombresConocidos: Map<String, String> = emptyMap()): ViajeGuardable {
    val entidad = ViajeEntity(
        folio = folio,
        estado = estado,
        correoSolicitante = correoSolicitante,
        nombreSolicitante = nombreSolicitante,
        telefonoSolicitante = telefonoSolicitante,
        razonSocial = razonSocial,
        origenDireccion = origen.direccion,
        origenComuna = origen.comuna,
        origenRegion = origen.region,
        origenLatitud = origen.latitud,
        origenLongitud = origen.longitud,
        destinoDireccion = destino.direccion,
        destinoComuna = destino.comuna,
        destinoRegion = destino.region,
        destinoLatitud = destino.latitud,
        destinoLongitud = destino.longitud,
        fhCreacion = FechaIso.texto(fhCreacion),
        fhAgenda = FechaIso.texto(fhAgenda),
        fhAsignacion = fhAsignacion?.let(FechaIso::texto),
        fhInicio = fhInicio?.let(FechaIso::texto),
        fhFin = fhFin?.let(FechaIso::texto),
        numeroVuelo = numeroVuelo,
        cantidadPasajeros = cantidadPasajeros,
        correoConductor = correoConductor,
        nombreConductor = nombreConductor,
        patenteMovil = patenteMovil,
        hashPago = hashPago,
        estadoPago = estadoPago,
        motivo = motivo,
        folioCorrelativo = folio.substringAfter('-').toLong(),
        descargadoEn = descargadoEn,
    )
    return ViajeGuardable(
        viaje = entidad,
        pasajeros = pasajeros.map { PasajeroEntity(it.dniHash, it.nombre ?: nombresConocidos[it.dniHash].orEmpty()) },
        detalles = pasajeros.map { ViajePasajeroEntity(folio, it.dniHash, it.fhAbordaje?.let(FechaIso::texto)) },
    )
}

private fun seisDecimales(valor: Double): Double = round(valor * 1_000_000) / 1_000_000
