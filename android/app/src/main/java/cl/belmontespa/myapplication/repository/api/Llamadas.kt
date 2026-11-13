package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.Resultado
import kotlinx.coroutines.CancellationException

internal suspend fun <T> intentar(bloque: suspend () -> Resultado<T>): Resultado<T> = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Resultado.Fallo(ErrorMapper.aErrorApp(e))
}

internal suspend fun <T> llamar(bloque: suspend () -> T): Resultado<T> = intentar { Resultado.Exito(bloque()) }
