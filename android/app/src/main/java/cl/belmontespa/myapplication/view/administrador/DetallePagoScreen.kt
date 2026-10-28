package cl.belmontespa.myapplication.view.administrador

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.permiteCambioA
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.viewmodel.administrador.DetallePagoViewModel

@Composable
fun DetallePagoScreen(
    hash: String,
    onVolver: () -> Unit,
    viewModel: DetallePagoViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(hash) { viewModel.cargar(hash, forzar = true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alVolverAPrimerPlano() }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.DETALLE_PAGO.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
        onReintentar = viewModel::reintentar,
    ) {
        val pago = estado.pago ?: return@ContenedorPantalla
        var confirmar by remember(pago.hash, pago.estado) { mutableStateOf<EstadoPago?>(null) }
        Dato(R.string.campo_folio, pago.folio)
        Dato(R.string.campo_monto, Formatos.monto(pago.monto))
        EstadoChip(stringResource(pago.estado.etiqueta), pago.estado.estiloChip)
        Dato(R.string.campo_banco, pago.banco)
        Dato(R.string.campo_medio_pago, stringResource(pago.medioPago.etiqueta))
        Dato(R.string.campo_tipo_recibo, stringResource(pago.tipoRecibo.etiqueta))
        Dato(R.string.pago_fecha_label, pago.fechaPago?.let(Formatos::fechaHoraDetalle) ?: stringResource(R.string.pago_fecha_pendiente))
        pago.montoReembolsado?.let { Dato(R.string.campo_reembolso, Formatos.monto(it)) }
        if (estado.esAdministrador) {
            val sinError = estado.error == null
            if (pago.estado == EstadoPago.PENDIENTE) {
                Accion(R.string.accion_pagar, { confirmar = EstadoPago.PAGADO }, habilitada = sinError, enviando = estado.enviando)
                Accion(R.string.accion_rechazar, { confirmar = EstadoPago.RECHAZADO }, TipoBoton.SECUNDARIO, sinError, estado.enviando)
            }
            if (pago.estado.permiteCambioA(EstadoPago.ANULADO)) {
                Accion(R.string.accion_anular, { confirmar = EstadoPago.ANULADO }, TipoBoton.PELIGRO, sinError, estado.enviando)
            }
            confirmar?.let { nuevo ->
                AlertDialog(
                    onDismissRequest = { confirmar = null },
                    title = { Text(stringResource(R.string.pago_confirmar_titulo, stringResource(nuevo.etiqueta))) },
                    text = {
                        Text(
                            stringResource(
                                when (nuevo) {
                                    EstadoPago.PAGADO -> R.string.pago_confirmar_pagado
                                    EstadoPago.RECHAZADO -> R.string.pago_confirmar_rechazado
                                    else -> R.string.pago_confirmar_anulado
                                },
                            ),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { confirmar = null; viewModel.cambiarEstado(nuevo) },
                            enabled = !estado.enviando && sinError,
                        ) { Text(stringResource(R.string.accion_confirmar)) }
                    },
                    dismissButton = { TextButton(onClick = { confirmar = null }) { Text(stringResource(R.string.accion_cancelar)) } },
                )
            }
        }
    }
}
