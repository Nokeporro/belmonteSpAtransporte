package cl.belmontespa.myapplication.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.CampoFormulario
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/**
 * «Cambiar contraseña», o «Cambio obligatorio de contraseña» si `pantalla` es [PantallaDemo.CAMBIO_OBLIGATORIO]:
 * esa variante avisa que hay que cambiarla y ofrece cerrar sesión.
 */
@Composable
fun CambiarPasswordScreen(
    pantalla: PantallaDemo,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: CambiarPasswordViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.cargar() }
    val obligatorio = pantalla == PantallaDemo.CAMBIO_OBLIGATORIO
    ContenedorPantalla(
        titulo = stringResource(pantalla.titulo),
        cargando = estado.cargando,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
        onReintentar = viewModel::reintentar,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        if (obligatorio) Aviso(stringResource(R.string.aviso_cambio_password_obligatorio), TonoAviso.AVISO)
        CampoFormulario(estado.passwordActual, viewModel::escribirActual, R.string.campo_password_actual, "passwordActual", e, enviando, KeyboardType.Password)
        CampoFormulario(estado.passwordNueva, viewModel::escribirNueva, R.string.campo_password_nueva, "passwordNueva", e, enviando, KeyboardType.Password)
        CampoFormulario(estado.confirmacion, viewModel::escribirConfirmacion, R.string.campo_confirmacion, "confirmacion", e, enviando, KeyboardType.Password)
        Accion(R.string.accion_guardar, viewModel::guardar, enviando = enviando)
        if (obligatorio) Accion(R.string.accion_cerrar_sesion, onCerrarSesion, TipoBoton.SECUNDARIO)
    }
}
