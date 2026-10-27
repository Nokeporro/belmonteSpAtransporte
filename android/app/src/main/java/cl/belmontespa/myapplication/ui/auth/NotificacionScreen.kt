package cl.belmontespa.myapplication.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.Bloque
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.Dato
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos

/** «Notificación push»: cada viaje como un aviso de «nuevo viaje» que abre el detalle que corresponde al rol. */
@Composable
fun NotificacionScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: NotificacionViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.cargar() }
    val detalle = when (estado.cuenta?.rol) {
        Rol.ADMINISTRADOR -> PantallaDemo.DETALLE_ADMIN
        Rol.CONDUCTOR -> PantallaDemo.DETALLE_VIAJE
        else -> PantallaDemo.DETALLE_SOLICITUD
    }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.NOTIFICACION.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        onReintentar = viewModel::reintentar,
    ) {
        Aviso(stringResource(R.string.aviso_notificacion), TonoAviso.INFORMACION)
        estado.viajes.forEach { viaje ->
            Bloque(stringResource(R.string.notificacion_nuevo_viaje)) {
                Dato(R.string.campo_folio, viaje.folio)
                Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraDetalle(viaje.fhAgenda))
                Accion(R.string.accion_ver_viaje, { onNavegar(detalle, viaje.folio) }, TipoBoton.SECUNDARIO)
            }
        }
        // Los escenarios son explícitos y solo existen en la build demo.
        // No se presentan como notificaciones push recibidas del servidor.
        val primero = estado.viajes.firstOrNull()
        if (BuildConfig.MODO_LOCAL && estado.cuenta?.rol == Rol.CONDUCTOR && primero != null) {
            Accion(R.string.t06_sin_conexion, { onNavegar(PantallaDemo.SIN_CONEXION, primero.folio) }, TipoBoton.SECUNDARIO)
            Accion(R.string.t06_conflicto, { onNavegar(PantallaDemo.CONFLICTO, primero.folio) }, TipoBoton.SECUNDARIO)
        }
    }
}
