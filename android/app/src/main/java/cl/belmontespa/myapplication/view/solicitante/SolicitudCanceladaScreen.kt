package cl.belmontespa.myapplication.view.solicitante

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.solicitante.SolicitudCanceladaViewModel

/** «Solicitud cancelada»: confirma la cancelación y vuelve a «Mis solicitudes». */
@Composable
fun SolicitudCanceladaScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: SolicitudCanceladaViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.SOLICITUD_CANCELADA.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        onReintentar = viewModel::reintentar,
    ) {
        val viaje = estado.viaje
        if (viaje?.estado == EstadoViaje.CANCELADO) {
            Aviso(stringResource(R.string.aviso_solicitud_cancelada), TonoAviso.INFORMACION)
            PagoDeCancelacion(viaje, estado.pago, confirmando = false)
        } else {
            Aviso(stringResource(R.string.error_no_encontrado), TonoAviso.AVISO)
        }
        Dato(R.string.campo_folio, viaje?.folio)
        Accion(R.string.accion_volver_solicitudes, { onNavegar(PantallaDemo.MIS_SOLICITUDES, "") }, TipoBoton.SECUNDARIO)
    }
}
