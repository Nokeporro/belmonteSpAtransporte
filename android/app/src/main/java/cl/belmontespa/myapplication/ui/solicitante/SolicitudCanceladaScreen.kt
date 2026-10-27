package cl.belmontespa.myapplication.ui.solicitante

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.Dato
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Solicitud cancelada»: confirma la cancelación y vuelve a «Mis solicitudes». */
@Composable
fun SolicitudCanceladaScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: SolicitudCanceladaViewModel = hiltViewModel(),
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
