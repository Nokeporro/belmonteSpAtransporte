package cl.belmontespa.myapplication.model

/** Respuesta de toda operación `suspend` de un repositorio. */
sealed interface Resultado<out T> {
    data class Exito<out T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ErrorApp) : Resultado<Nothing>

    /** Solo "Iniciar viaje" y "Finalizar viaje" sin conexión, en la implementación remota. */
    data object Encolado : Resultado<Nothing>
}

/** Errores que ven los ViewModel; ambas implementaciones (local y remota) devuelven los mismos. */
sealed interface ErrorApp {
    /** 400: un motivo por campo, con el nombre del contrato JSON (`origen.latitud`). */
    data class Validacion(val errores: Map<String, String>) : ErrorApp

    data object SinConexion : ErrorApp

    /** 401: con sesión, el token venció; en el login, las credenciales no son válidas. */
    data object SesionExpirada : ErrorApp

    /** 403, con el motivo que decide qué texto se muestra. */
    data class SinPermiso(val motivo: MotivoSinPermiso = MotivoSinPermiso.GENERAL) : ErrorApp

    /** 404. */
    data object NoEncontrado : ErrorApp

    /** 409, con el `message` del backend (o el mismo texto en la build demo). */
    data class Conflicto(val mensaje: String) : ErrorApp

    data object Inesperado : ErrorApp
}

enum class MotivoSinPermiso { GENERAL, CUENTA_PENDIENTE, CUENTA_RECHAZADA, CAMBIAR_PASSWORD }
