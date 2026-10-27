package cl.belmontespa.myapplication.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.CampoFormulario
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Registro de solicitante»: al terminar ofrece volver al Login con el correo ya escrito. */
@Composable
fun RegistroScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistroViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.REGISTRO.titulo),
        cargando = false,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        CampoFormulario(estado.nombre, viewModel::escribirNombre, R.string.campo_nombre, "nombre", e, enviando)
        CampoFormulario(estado.correo, viewModel::escribirCorreo, R.string.campo_correo, "correo", e, enviando, KeyboardType.Email)
        CampoFormulario(estado.telefono, viewModel::escribirTelefono, R.string.campo_telefono, "telefono", e, enviando, KeyboardType.Phone)
        CampoFormulario(estado.password, viewModel::escribirPassword, R.string.campo_password, "password", e, enviando, KeyboardType.Password)
        CampoFormulario(estado.razonSocial, viewModel::escribirRazonSocial, R.string.campo_razon_social, "razonSocial", e, enviando)
        CampoFormulario(estado.rutEmpresa, viewModel::escribirRutEmpresa, R.string.campo_rut_empresa, "rutEmpresa", e, enviando)
        Accion(R.string.login_crear_cuenta, viewModel::registrar, enviando = enviando)
        if (estado.registrado) {
            Accion(R.string.login_iniciar_sesion, { onNavegar(PantallaDemo.LOGIN, estado.correo) }, TipoBoton.SECUNDARIO)
        }
    }
}
