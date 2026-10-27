package cl.belmontespa.myapplication.view.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.auth.CambiarPasswordViewModel

/**
 * «Cambiar contraseña», o «Cambio obligatorio de contraseña» si `pantalla` es [PantallaDemo.CAMBIO_OBLIGATORIO]:
 * esa variante avisa que hay que cambiarla y ofrece cerrar sesión.
 */
@Composable
fun CambiarPasswordScreen(
    pantalla: PantallaDemo,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: CambiarPasswordViewModel = viewModel(),
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
