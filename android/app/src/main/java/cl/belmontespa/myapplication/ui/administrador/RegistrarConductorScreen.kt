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

/** «Registrar conductor»: al guardar abre el detalle del conductor creado. */
@Composable
fun RegistrarConductorScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistrarConductorViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.registrado) {
        estado.registrado?.let { correo -> viewModel.consumirRegistrado(); onNavegar(PantallaDemo.DETALLE_CONDUCTOR, correo) }
    }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.REGISTRAR_CONDUCTOR.titulo),
        cargando = false,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        CampoFormulario(estado.nombre, viewModel::escribirNombre, R.string.campo_nombre, "nombre", e, enviando)
        CampoFormulario(estado.correo, viewModel::escribirCorreo, R.string.campo_correo, "correo", e, enviando, KeyboardType.Email)
        CampoFormulario(estado.telefono, viewModel::escribirTelefono, R.string.campo_telefono, "telefono", e, enviando, KeyboardType.Phone)
        CampoFormulario(estado.password, viewModel::escribirPassword, R.string.campo_password, "password", e, enviando, KeyboardType.Password)
        Text(stringResource(R.string.conductor_alta_ayuda))
        Accion(R.string.accion_guardar, viewModel::guardar, enviando = enviando)
    }
}
