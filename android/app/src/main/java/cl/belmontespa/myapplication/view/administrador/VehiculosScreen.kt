package cl.belmontespa.myapplication.view.administrador

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.viewmodel.administrador.VehiculosViewModel

@Composable
fun VehiculosScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    viewModel: VehiculosViewModel = viewModel(),
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
