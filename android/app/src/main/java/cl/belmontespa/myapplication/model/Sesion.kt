package cl.belmontespa.myapplication.model

enum class Rol { SOLICITANTE, ADMINISTRADOR, CONDUCTOR }

data class Sesion(
    val token: String,
    val correo: String,
    val rol: Rol,
    val expiraEn: Long,
)

sealed interface EstadoSesion {
    data object SinSesion : EstadoSesion
    data class Temporal(val sesion: Sesion) : EstadoSesion
    data class Autenticada(val sesion: Sesion) : EstadoSesion
}
