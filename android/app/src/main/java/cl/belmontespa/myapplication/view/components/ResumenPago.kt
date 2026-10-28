package cl.belmontespa.myapplication.view.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.util.Formatos

@Composable
fun ResumenPago(pago: Pago) {
    Bloque(stringResource(R.string.campo_pago)) {
        EstadoChip(stringResource(pago.estado.etiqueta), pago.estado.estiloChip)
        Dato(R.string.campo_monto, Formatos.monto(pago.monto))
        pago.montoReembolsado?.let { Dato(R.string.campo_monto_reembolsado, Formatos.monto(it)) }
    }
}
