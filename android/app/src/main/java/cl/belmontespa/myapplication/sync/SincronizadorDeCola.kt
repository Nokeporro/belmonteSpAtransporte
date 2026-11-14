package cl.belmontespa.myapplication.sync

import androidx.room.withTransaction
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.OperacionPendienteEntity
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.api.ApiClient
import cl.belmontespa.myapplication.repository.api.CacheViajes
import cl.belmontespa.myapplication.repository.api.ErrorMapper
import cl.belmontespa.myapplication.repository.dto.EstadoViajeRequest
import cl.belmontespa.myapplication.repository.dto.ViajeResponse
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

enum class ResultadoSync { LISTO, REINTENTAR, ESPERAR_SESION }

internal enum class SalidaDeEnvio { REINTENTAR, ESPERAR_SESION, VERIFICAR, CONFLICTO }

internal fun clasificar(error: Throwable): SalidaDeEnvio = when {
    error is HttpException -> when (error.code()) {
        401 -> SalidaDeEnvio.ESPERAR_SESION
        409 -> SalidaDeEnvio.VERIFICAR
        in 500..599 -> SalidaDeEnvio.REINTENTAR
        else -> SalidaDeEnvio.CONFLICTO
    }
    error is IOException -> SalidaDeEnvio.REINTENTAR
    else -> SalidaDeEnvio.CONFLICTO
}

class SincronizadorDeCola(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)
    private val cache = CacheViajes(local)
    private val cola get() = local.db.operacionPendienteDao()

    suspend fun enviar(): ResultadoSync {
        val sesion = (local.sesiones.estado.first() as? EstadoSesion.Autenticada)?.sesion ?: return ResultadoSync.ESPERAR_SESION
        val detenidos = cola.foliosEnConflicto(sesion.correo).toMutableSet()
        for (operacion in cola.pendientesDeLaCuenta(sesion.correo)) {
            if (operacion.folio in detenidos) continue
            when (enviarUna(operacion)) {
                Paso.SEGUIR -> Unit
                Paso.DETENER_VIAJE -> detenidos += operacion.folio
                Paso.REINTENTAR -> return ResultadoSync.REINTENTAR
                Paso.ESPERAR_SESION -> return ResultadoSync.ESPERAR_SESION
            }
        }
        return ResultadoSync.LISTO
    }

    private enum class Paso { SEGUIR, DETENER_VIAJE, REINTENTAR, ESPERAR_SESION }

    private suspend fun enviarUna(operacion: OperacionPendienteEntity): Paso {
        val viaje = try {
            api.cambiarEstadoViaje(operacion.folio, EstadoViajeRequest(operacion.estadoDestino))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return alFallar(operacion, e)
        }
        aplicar(operacion, viaje)
        return Paso.SEGUIR
    }

    private suspend fun alFallar(operacion: OperacionPendienteEntity, error: Exception): Paso {
        val mensaje = (error as? HttpException)?.let { ErrorMapper.mensajeDe(it.response()?.errorBody()?.string()) }
        return when (clasificar(error)) {
            SalidaDeEnvio.REINTENTAR -> Paso.REINTENTAR
            SalidaDeEnvio.ESPERAR_SESION -> Paso.ESPERAR_SESION
            SalidaDeEnvio.VERIFICAR -> verificar(operacion, mensaje)
            SalidaDeEnvio.CONFLICTO -> conflicto(operacion, mensaje)
        }
    }

    private suspend fun verificar(operacion: OperacionPendienteEntity, mensaje: String?): Paso {
        val actual = try {
            api.viaje(operacion.folio)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return when (clasificar(e)) {
                SalidaDeEnvio.REINTENTAR -> Paso.REINTENTAR
                SalidaDeEnvio.ESPERAR_SESION -> Paso.ESPERAR_SESION
                else -> conflicto(operacion, mensaje)
            }
        }
        if (actual.estado == operacion.estadoDestino) {
            aplicar(operacion, actual)
            return Paso.SEGUIR
        }
        cache.guardar(listOf(actual))
        return conflicto(operacion, mensaje)
    }

    private suspend fun aplicar(operacion: OperacionPendienteEntity, viaje: ViajeResponse) {
        local.db.withTransaction {
            cache.guardar(listOf(viaje))
            cola.borrar(operacion.id)
        }
    }

    private suspend fun conflicto(operacion: OperacionPendienteEntity, mensaje: String?): Paso {
        cola.marcarConflicto(operacion.id, mensaje)
        return Paso.DETENER_VIAJE
    }
}
