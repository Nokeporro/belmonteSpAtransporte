package cl.belmontespa.myapplication.view.administrador

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.viewmodel.administrador.DetalleVehiculoViewModel

/** «Detalle de vehículo»: datos, código de abordaje (solo lo ve el Administrador), habilitados y baja. */
@Composable
fun DetalleVehiculoScreen(
    patente: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: DetalleVehiculoViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Al volver del alta o de editar, el detalle debe leer los datos guardados.
    LaunchedEffect(patente) { viewModel.cargar(patente, forzar = true) }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.DETALLE_VEHICULO.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
        onReintentar = viewModel::reintentar,
    ) {
        val movil = estado.movil ?: return@ContenedorPantalla
        Dato(R.string.campo_patente, movil.patente)
        Dato(R.string.campo_capacidad, movil.capacidad.toString())
        Dato(R.string.campo_gps, movil.gpsSerial?.toString() ?: stringResource(R.string.vehiculo_sin_gps))
        EstadoChip(stringResource(movil.estado.etiqueta(movil.activo)), movil.estado.estiloChip(movil.activo))
        ResumenHabilitaciones(estado.habilitaciones)
        Accion(R.string.accion_habilitaciones, { onNavegar(PantallaDemo.HABILITACIONES, "vehiculo:${movil.patente}") }, TipoBoton.SECUNDARIO)
        Dato(R.string.campo_codigo, Formatos.codigoAbordaje(movil.codigoAbordaje))
        CambioActivo(
            movil.patente, movil.activo, habilitado = estado.error == null, enviando = estado.enviando,
            onConfirmar = viewModel::cambiarActivo,
        )
    }
}
