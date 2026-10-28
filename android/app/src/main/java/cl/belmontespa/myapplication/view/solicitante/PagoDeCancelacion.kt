package cl.belmontespa.myapplication.view.solicitante

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta

@Composable
internal fun PagoDeCancelacion(viaje: Viaje, pago: Pago?, confirmando: Boolean) {
    if (pago == null) {
        if (viaje.hashPago == null) Text(stringResource(R.string.aviso_sin_pago))
        return
    }
    EstadoChip(stringResource(pago.estado.etiqueta), pago.estado.estiloChip)
    when {
        confirmando && pago.estado == EstadoPago.PAGADO -> Aviso(
            stringResource(R.string.cancelacion_reembolso_previsto, Formatos.monto(pago.monto / 2)), TonoAviso.INFORMACION,
        )
        confirmando && pago.estado == EstadoPago.PENDIENTE -> Text(stringResource(R.string.cancelacion_anula_pago))
        !confirmando && pago.estado == EstadoPago.REEMBOLSADO ->
            pago.montoReembolsado?.let { Dato(R.string.campo_reembolso, Formatos.monto(it)) }
        !confirmando && pago.estado == EstadoPago.ANULADO -> Text(stringResource(R.string.cancelacion_pago_anulado))
    }
}
