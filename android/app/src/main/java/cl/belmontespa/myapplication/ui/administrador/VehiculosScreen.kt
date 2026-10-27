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

/** «Vehículos»: raíz de la barra inferior del Administrador. */
@Composable
fun VehiculosScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    viewModel: VehiculosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.cargar() }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.VEHICULOS.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = null,
        error = estado.error,
        onReintentar = viewModel::cargar,
    ) {
        Accion(R.string.t06_registrar_vehiculo, { onNavegar(PantallaDemo.REGISTRAR_VEHICULO, "") }, TipoBoton.SECUNDARIO)
        if (estado.moviles.isEmpty()) Text(stringResource(R.string.aviso_sin_resultados))
        estado.moviles.forEach { movil ->
            Bloque(movil.patente) {
                Dato(R.string.campo_capacidad, movil.capacidad.toString())
                EstadoChip(stringResource(movil.estado.etiqueta(movil.activo)), movil.estado.estiloChip(movil.activo))
                Accion(R.string.accion_ver_detalle, { onNavegar(PantallaDemo.DETALLE_VEHICULO, movil.patente) }, TipoBoton.SECUNDARIO)
            }
        }
    }
}
