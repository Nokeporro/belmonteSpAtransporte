package cl.belmontespa.myapplication.ui.administrador

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.CampoFormulario
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Registrar administrador»: el alta se confirma en la misma pantalla, que conserva los datos guardados. */
@Composable
fun RegistrarAdminScreen(
    onVolver: () -> Unit,
    viewModel: RegistrarAdminViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.cargar() }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.REGISTRAR_ADMIN.titulo),
        cargando = estado.cargando,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
        onReintentar = viewModel::reintentar,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        CampoFormulario(estado.nombre, viewModel::escribirNombre, R.string.campo_nombre, "nombre", e, enviando)
        CampoFormulario(estado.correo, viewModel::escribirCorreo, R.string.campo_correo, "correo", e, enviando, KeyboardType.Email)
        CampoFormulario(estado.telefono, viewModel::escribirTelefono, R.string.campo_telefono, "telefono", e, enviando, KeyboardType.Phone)
        CampoFormulario(estado.password, viewModel::escribirPassword, R.string.admin_password_inicial, "password", e, enviando, KeyboardType.Password)
        Text(stringResource(R.string.admin_alta_ayuda))
        Accion(R.string.accion_guardar, viewModel::guardar, enviando = enviando)
    }
}
