package cl.belmontespa.myapplication.model

sealed interface Resultado<out T> {
    data class Exito<out T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ErrorApp) : Resultado<Nothing>

    data object Encolado : Resultado<Nothing>
}

sealed interface ErrorApp {
    data class Validacion(val errores: Map<String, String>) : ErrorApp

    data object SinConexion : ErrorApp

    data object SesionExpirada : ErrorApp

    data class SinPermiso(val motivo: MotivoSinPermiso = MotivoSinPermiso.GENERAL) : ErrorApp

    data object NoEncontrado : ErrorApp

    data class Conflicto(val mensaje: String) : ErrorApp

    data object Inesperado : ErrorApp
}

enum class MotivoSinPermiso { GENERAL, CUENTA_PENDIENTE, CUENTA_RECHAZADA, CAMBIAR_PASSWORD }
