package cl.belmontespa.myapplication.view.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.auth.RecuperarPasswordViewModel

/** «Recuperar contraseña»: en la build `demo` avisa que no se envía ningún correo. */
@Composable
fun RecuperarPasswordScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RecuperarPasswordViewModel = viewModel(),
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
