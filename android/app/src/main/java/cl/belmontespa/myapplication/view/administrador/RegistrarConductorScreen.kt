package cl.belmontespa.myapplication.view.administrador

import androidx.compose.material3.Text
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
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.viewmodel.administrador.RegistrarConductorViewModel

/** «Registrar conductor»: al guardar abre el detalle del conductor creado. */
@Composable
fun RegistrarConductorScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistrarConductorViewModel = viewModel(),
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
