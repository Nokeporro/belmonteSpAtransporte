package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.AsignarRequest
import cl.belmontespa.myapplication.repository.dto.CancelarRequest
import cl.belmontespa.myapplication.repository.dto.EstadoViajeRequest
import cl.belmontespa.myapplication.repository.dto.PasajeroViajeRequest
import cl.belmontespa.myapplication.repository.dto.RechazarRequest
import cl.belmontespa.myapplication.repository.dto.ViajeRequest
import cl.belmontespa.myapplication.repository.dto.aDto
import cl.belmontespa.myapplication.repository.dto.aRequest
import cl.belmontespa.myapplication.repository.obtener
import cl.belmontespa.myapplication.util.Validaciones
import kotlinx.coroutines.CancellationException

internal class ViajeRemoto(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)
    private val cache = CacheViajes(local)

    suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = llamar {
        cache.guardar(api.viajes(filtro.estado?.name, filtro.fecha?.toString()))
    }

    suspend fun refrescarViaje(folio: String): Resultado<Unit> = cache.refrescar(folio)

    suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = intentar {
        val dniHashes = solicitud.pasajeros.map { api.registrarPasajero(it.aRequest()).dniHash }
        val viaje = api.crearViaje(
            ViajeRequest(solicitud.fhAgenda, solicitud.origen.aDto(), solicitud.destino.aDto(), solicitud.numeroVuelo),
        )
        try {
            dniHashes.forEach { api.agregarPasajero(PasajeroViajeRequest(viaje.folio, it)) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        cache.guardar(listOf(viaje))
        cache.refrescar(viaje.folio)
        Resultado.Exito(viaje.folio)
    }

    suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = intentar {
        val dniHash = api.registrarPasajero(pasajero.aRequest()).dniHash
        api.agregarPasajero(PasajeroViajeRequest(folio, dniHash))
        cache.refrescar(folio)
        Resultado.Exito(Unit)
    }

    suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = llamar {
        val viaje = api.asignar(folio, AsignarRequest(Validaciones.correo(correoConductor), Validaciones.patente(patente)))
        cache.guardar(listOf(viaje))
    }

    suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> = intentar {
        val sesion = local.sesion().obtener { return@intentar it }
        val viaje = if (sesion.rol == Rol.ADMINISTRADOR) {
            api.cancelarComoAdministrador(folio, CancelarRequest(motivo.orEmpty().trim(), montoReembolso))
        } else {
            api.cancelarComoSolicitante(folio)
        }
        cache.guardar(listOf(viaje))
        Resultado.Exito(Unit)
    }

    suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = llamar {
        cache.guardar(listOf(api.rechazar(folio, RechazarRequest(motivo.trim()))))
    }

    suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> = llamar {
        cache.guardar(listOf(api.cambiarEstadoViaje(folio, EstadoViajeRequest(estado.name))))
    }
}
