package cl.belmontespa.myapplication.ui

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import kotlinx.coroutines.CancellationException

/** Corta una operación con el error que verá la pantalla; [capturar] lo atrapa. */
internal class FalloPantalla(val error: ErrorApp) : RuntimeException()

/** El valor de un resultado exitoso; un fallo interrumpe el bloque que lo pidió. */
internal fun <T> Resultado<T>.valor(): T = when (this) {
    is Resultado.Exito -> valor
    is Resultado.Fallo -> throw FalloPantalla(error)
    Resultado.Encolado -> throw FalloPantalla(ErrorApp.Conflicto("El cambio está pendiente de sincronizar."))
}

/** Lo que ve el formulario si una operación termina con una excepción que no es de la aplicación. */
internal val DATOS_INVALIDOS = ErrorApp.Validacion(mapOf("datos" to "Revisa los datos ingresados."))

/** Ejecuta [bloque] y devuelve el error con que terminó, o `null` si salió bien. */
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
