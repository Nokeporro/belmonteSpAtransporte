package cl.belmontespa.myapplication.view.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.auth.NotificacionViewModel

/** «Notificación push»: cada viaje como un aviso de «nuevo viaje» que abre el detalle que corresponde al rol. */
@Composable
fun NotificacionScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: NotificacionViewModel = viewModel(),
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
