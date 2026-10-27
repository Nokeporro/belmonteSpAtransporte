package cl.belmontespa.myapplication.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.CampoFormulario
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Recuperar contraseña»: en la build `demo` avisa que no se envía ningún correo. */
@Composable
fun RecuperarPasswordScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RecuperarPasswordViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.RECUPERAR.titulo),
        cargando = false,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
    ) {
        if (BuildConfig.MODO_LOCAL) Aviso(stringResource(R.string.recuperacion_demo), TonoAviso.INFORMACION)
        CampoFormulario(
            estado.correo, viewModel::escribirCorreo, R.string.campo_correo, "correo", estado.error, estado.enviando,
            KeyboardType.Email,
        )
        Accion(R.string.login_recuperar_password, viewModel::recuperar, habilitada = !estado.enviado, enviando = estado.enviando)
        if (estado.enviado) {
            Accion(R.string.login_iniciar_sesion, { onNavegar(PantallaDemo.LOGIN, estado.correo) }, TipoBoton.SECUNDARIO)
        }
    }
}
