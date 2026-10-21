package cl.belmontespa.myapplication.model

/** Rol que entrega el login; decide el flujo de navegación. */
enum class Rol { SOLICITANTE, ADMINISTRADOR, CONDUCTOR }

/** Sesión guardada: el token, la cuenta, su rol y cuándo vence (milisegundos Unix). */
data class Sesion(
    val token: String,
    val correo: String,
    val rol: Rol,
    val expiraEn: Long,
)

/** SIN_SESION, TEMPORAL (entró con la contraseña temporal y debe cambiarla) o AUTENTICADA. */
sealed interface EstadoSesion {
    data object SinSesion : EstadoSesion
    data class Temporal(val sesion: Sesion) : EstadoSesion
    data class Autenticada(val sesion: Sesion) : EstadoSesion
}
