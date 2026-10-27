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
import cl.belmontespa.myapplication.ui.components.Dato
import cl.belmontespa.myapplication.ui.components.EstadoChip
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.estiloChip
import cl.belmontespa.myapplication.ui.components.etiqueta
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos

/** «Detalle de conductor»: sus datos, la edición, sus vehículos habilitados y desactivar o reactivar. */
@Composable
fun DetalleConductorScreen(
    correo: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: DetalleConductorViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Al volver del alta o de editar, el detalle debe leer los datos guardados.
    LaunchedEffect(correo) { viewModel.cargar(correo, forzar = true) }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.DETALLE_CONDUCTOR.titulo),
        cargando = estado.cargando,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
        onReintentar = viewModel::reintentar,
    ) {
        val conductor = estado.conductor ?: return@ContenedorPantalla
        val e = estado.error
        val enviando = estado.enviando
        Dato(R.string.campo_correo, conductor.correo)
        EstadoChip(stringResource(conductor.estado.etiqueta(conductor.activo)), conductor.estado.estiloChip(conductor.activo))
        ResumenAgendaConductor(conductor)
        Dato(R.string.campo_fecha_registro, Formatos.fechaHoraDetalle(conductor.fechaRegistro))
        CampoFormulario(estado.nombre, viewModel::escribirNombre, R.string.campo_nombre, "nombre", e, enviando)
        CampoFormulario(estado.telefono, viewModel::escribirTelefono, R.string.campo_telefono, "telefono", e, enviando, KeyboardType.Phone)
        CampoFormulario(estado.password, viewModel::escribirPassword, R.string.campo_password_nueva, "password", e, enviando, KeyboardType.Password)
        Text(stringResource(R.string.conductor_password_opcional))
        Accion(R.string.accion_guardar, viewModel::guardar, enviando = enviando)
        if (estado.habilitaciones.isEmpty()) Text(stringResource(R.string.conductor_sin_habilitaciones))
        ResumenHabilitaciones(estado.habilitaciones)
        Accion(R.string.accion_habilitaciones, { onNavegar(PantallaDemo.HABILITACIONES, "conductor:${conductor.correo}") }, TipoBoton.SECUNDARIO)
        CambioActivo(conductor.correo, conductor.activo, habilitado = e == null, enviando = enviando, onConfirmar = viewModel::cambiarActivo)
    }
}
