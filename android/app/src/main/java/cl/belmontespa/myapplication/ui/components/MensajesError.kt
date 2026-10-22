package cl.belmontespa.myapplication.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.MotivoSinPermiso

// Texto de un error de pantalla (README, Textos y tema). El login tiene el suyo (`mensajeLogin`),
// porque allí un 401 son credenciales inválidas y no una sesión terminada.

/** Un 409 trae el `message` del backend; el resto sale de `strings.xml`. */
@Composable
fun ErrorApp.texto(): String = when (this) {
    is ErrorApp.Conflicto -> mensaje
    else -> stringResource(mensajeRes())
}

@StringRes
fun ErrorApp.mensajeRes(): Int = when (this) {
    ErrorApp.SinConexion -> R.string.error_sin_conexion
    ErrorApp.SesionExpirada -> R.string.error_sesion_terminada
    is ErrorApp.SinPermiso -> when (motivo) {
        MotivoSinPermiso.CUENTA_PENDIENTE -> R.string.error_cuenta_pendiente
        MotivoSinPermiso.CUENTA_RECHAZADA -> R.string.error_cuenta_rechazada
        MotivoSinPermiso.CAMBIAR_PASSWORD -> R.string.error_debes_cambiar_password
        MotivoSinPermiso.GENERAL -> R.string.error_sin_permiso
    }
    ErrorApp.NoEncontrado -> R.string.error_no_encontrado
    is ErrorApp.Validacion -> R.string.error_revisa_datos
    is ErrorApp.Conflicto, ErrorApp.Inesperado -> R.string.error_inesperado
}
