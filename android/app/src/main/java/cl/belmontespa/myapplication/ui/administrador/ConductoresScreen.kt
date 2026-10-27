package cl.belmontespa.myapplication.ui.administrador

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.Bloque
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.Dato
import cl.belmontespa.myapplication.ui.components.EstadoChip
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.estiloChip
import cl.belmontespa.myapplication.ui.components.etiqueta
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Conductores»: raíz de la barra inferior del Administrador. */
@Composable
fun ConductoresScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    viewModel: ConductoresViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.cargar() }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.CONDUCTORES.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = null,
        error = estado.error,
        onReintentar = viewModel::cargar,
    ) {
        Accion(R.string.t06_registrar_conductor, { onNavegar(PantallaDemo.REGISTRAR_CONDUCTOR, "") }, TipoBoton.SECUNDARIO)
        if (estado.conductores.isEmpty()) Text(stringResource(R.string.vacio_conductores_disponibles))
        estado.conductores.forEach { conductor ->
            Bloque(conductor.nombre) {
                Dato(R.string.campo_correo, conductor.correo)
                EstadoChip(stringResource(conductor.estado.etiqueta(conductor.activo)), conductor.estado.estiloChip(conductor.activo))
                ResumenAgendaConductor(conductor)
                Accion(R.string.accion_ver_detalle, { onNavegar(PantallaDemo.DETALLE_CONDUCTOR, conductor.correo) }, TipoBoton.SECUNDARIO)
            }
        }
    }
}
