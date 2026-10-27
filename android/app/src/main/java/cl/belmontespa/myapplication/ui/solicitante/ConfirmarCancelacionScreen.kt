package cl.belmontespa.myapplication.ui.solicitante

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.cancelablePorSolicitante
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.Dato
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.components.rememberAhora
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos

/** «Confirmar cancelación»: dice qué pasa con el pago y, al confirmar, abre «Solicitud cancelada». */
@Composable
fun ConfirmarCancelacionScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: ConfirmarCancelacionViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    LaunchedEffect(estado.cancelada) { if (estado.cancelada) onNavegar(PantallaDemo.SOLICITUD_CANCELADA, folio) }
    val ahora = rememberAhora(viewModel::ahora)
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.CONFIRMAR_CANCELACION.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        onReintentar = viewModel::reintentar,
    ) {
        val viaje = estado.viaje
        viaje?.let {
            Dato(R.string.campo_folio, it.folio)
            Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraDetalle(it.fhAgenda))
        }
        val cancelable = viaje?.cancelablePorSolicitante(ahora) == true
        if (viaje != null && !cancelable) Aviso(stringResource(R.string.cancelacion_no_disponible), TonoAviso.AVISO)
        else Aviso(stringResource(R.string.aviso_cancelar_solicitud), TonoAviso.AVISO)
        if (viaje != null) PagoDeCancelacion(viaje, estado.pago, confirmando = true)
        Accion(
            R.string.accion_confirmar, viewModel::confirmar, TipoBoton.PELIGRO,
            habilitada = cancelable && estado.error == null && (viaje?.hashPago == null || estado.pago != null),
            enviando = estado.enviando,
        )
        Accion(R.string.accion_cancelar, onVolver, TipoBoton.SECUNDARIO)
    }
}
