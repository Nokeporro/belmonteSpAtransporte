package cl.belmontespa.myapplication.notificaciones

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Resultado

enum class ResultadoRegistroToken { LISTO, REINTENTAR }

suspend fun registrarTokenFcm(
    estado: EstadoSesion,
    token: String?,
    enviar: suspend (String) -> Resultado<Unit>,
): ResultadoRegistroToken {
    if (estado !is EstadoSesion.Autenticada || token.isNullOrBlank()) return ResultadoRegistroToken.LISTO
    return when (val resultado = enviar(token)) {
        is Resultado.Exito -> ResultadoRegistroToken.LISTO
        is Resultado.Fallo -> when (resultado.error) {
            ErrorApp.SinConexion, ErrorApp.Inesperado -> ResultadoRegistroToken.REINTENTAR
            else -> ResultadoRegistroToken.LISTO
        }
        Resultado.Encolado -> ResultadoRegistroToken.REINTENTAR
    }
}

internal fun debePedirPermisoNotificaciones(
    estado: EstadoSesion?, modoLocal: Boolean, configurado: Boolean,
    sdk: Int, concedido: Boolean, solicitado: Boolean,
): Boolean = estado is EstadoSesion.Autenticada && !modoLocal && configurado &&
    sdk >= 33 && !concedido && !solicitado
