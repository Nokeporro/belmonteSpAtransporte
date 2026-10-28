package cl.belmontespa.myapplication.viewmodel

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import kotlinx.coroutines.CancellationException

internal class FalloPantalla(val error: ErrorApp) : RuntimeException()

internal fun <T> Resultado<T>.valor(): T = when (this) {
    is Resultado.Exito -> valor
    is Resultado.Fallo -> throw FalloPantalla(error)
    Resultado.Encolado -> throw FalloPantalla(ErrorApp.Conflicto("El cambio está pendiente de sincronizar."))
}

internal val DATOS_INVALIDOS = ErrorApp.Validacion(mapOf("datos" to "Revisa los datos ingresados."))

internal suspend fun capturar(ante: ErrorApp, bloque: suspend () -> Unit): ErrorApp? = try {
    bloque()
    null
} catch (e: CancellationException) {
    throw e
} catch (e: FalloPantalla) {
    e.error
} catch (_: Exception) {
    ante
}
