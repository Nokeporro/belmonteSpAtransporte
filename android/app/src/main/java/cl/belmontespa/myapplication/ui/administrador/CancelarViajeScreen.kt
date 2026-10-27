package cl.belmontespa.myapplication.ui.administrador

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.BarraSuperior
import cl.belmontespa.myapplication.ui.components.BotonAncho
import cl.belmontespa.myapplication.ui.components.CampoTexto
import cl.belmontespa.myapplication.ui.components.EstadoChip
import cl.belmontespa.myapplication.ui.components.EtiquetaCalculada
import cl.belmontespa.myapplication.ui.components.PantallaCargando
import cl.belmontespa.myapplication.ui.components.PantallaError
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.components.estiloChip
import cl.belmontespa.myapplication.ui.components.etiqueta
import cl.belmontespa.myapplication.ui.components.texto
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import cl.belmontespa.myapplication.util.Formatos

/**
 * «Cancelar viaje» del Administrador (HU24, V7). Pide el motivo y, con el viaje vencido y el pago `PAGADO`, el monto a
 * reembolsar; «Confirmar» guarda y vuelve al detalle, que ya muestra el viaje cancelado y el efecto sobre el pago.
 */
@Composable
fun CancelarViajeScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: CancelarViajeViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var confirmarSalida by remember { mutableStateOf(false) }
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    LaunchedEffect(estado.cancelado) { if (estado.cancelado) onNavegar(PantallaDemo.DETALLE_ADMIN, folio) }
    // Con un motivo escrito, salir pide confirmar el descarte.
    BackHandler(enabled = estado.modificado) { confirmarSalida = true }
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            text = { Text(stringResource(R.string.aviso_descartar)) },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onVolver() }) { Text(stringResource(R.string.accion_descartar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text(stringResource(R.string.accion_seguir_editando)) }
            },
        )
    }
    CancelarViajeContenido(
        estado = estado,
        onMotivo = viewModel::escribirMotivo,
        onReembolso = viewModel::escribirReembolso,
        onConfirmar = viewModel::confirmar,
        onReintentar = viewModel::reintentar,
        onVolver = { if (estado.modificado) confirmarSalida = true else onVolver() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CancelarViajeContenido(
    estado: CancelarViajeUiState,
    onMotivo: (String) -> Unit,
    onReembolso: (String) -> Unit,
    onConfirmar: () -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
) {
    val viaje = estado.viaje
    val error = estado.error
    // Reserva el espacio del teclado para que los botones sigan siendo alcanzables.
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        BarraSuperior(
            titulo = stringResource(R.string.t06_cancelar_viaje),
            subtitulo = viaje?.let { "${it.folio} · ${it.nombreSolicitante}" },
            onVolver = onVolver,
        )
        when {
            estado.cargando -> PantallaCargando()
            // Sin viaje no hay nada que cancelar: el error ocupa la pantalla.
            viaje == null -> Box(Modifier.testTag("pantalla_error")) {
                PantallaError(texto = (error ?: ErrorApp.NoEncontrado).texto(), onReintentar = onReintentar)
            }
            else -> Column(
                modifier = Modifier
                    .testTag("pantalla_contenido")
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (error != null) {
                    Aviso(error.texto(), TonoAviso.PELIGRO, Modifier.testTag("pantalla_error"))
                    // Un 409 ya recargó los datos: reintentar solo tiene sentido si falló una lectura.
                    if (error !is ErrorApp.Conflicto) {
                        BotonAncho(stringResource(R.string.accion_reintentar), onReintentar, tipo = TipoBoton.SECUNDARIO)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip, grande = true)
                    if (estado.vencido) {
                        EstadoChip(stringResource(EtiquetaCalculada.VENCIDO.etiqueta), EtiquetaCalculada.VENCIDO.estiloChip, grande = true)
                    }
                }
                Dato(stringResource(R.string.campo_fecha_agenda), Formatos.fechaHoraDetalle(viaje.fhAgenda), mono = true)
                estado.pago?.let { pago ->
                    Column {
                        Text(stringResource(R.string.campo_pago), style = EstilosMono.etiqueta)
                        EstadoChip(stringResource(pago.estado.etiqueta), pago.estado.estiloChip)
                    }
                    Dato(stringResource(R.string.campo_monto), Formatos.monto(pago.monto), mono = true)
                }
                if (!estado.cancelable) {
                    Aviso(stringResource(R.string.cancelar_viaje_no_disponible), TonoAviso.AVISO)
                } else {
                    EfectoEnElPago(estado, tieneHash = viaje.hashPago != null)
                    Text(stringResource(R.string.aviso_cancelar_viaje))
                    CampoTexto(
                        valor = estado.motivo,
                        onCambio = onMotivo,
                        etiqueta = stringResource(R.string.campo_motivo),
                        error = estado.errores[CAMPO_MOTIVO],
                        habilitado = !estado.enviando,
                    )
                    if (estado.pideReembolso) {
                        CampoTexto(
                            valor = estado.reembolso,
                            onCambio = onReembolso,
                            etiqueta = stringResource(R.string.campo_reembolso),
                            error = estado.errores[CAMPO_REEMBOLSO],
                            tipoTeclado = KeyboardType.Number,
                            habilitado = !estado.enviando,
                        )
                    }
                }
                BotonAncho(
                    texto = stringResource(R.string.accion_confirmar),
                    onClick = onConfirmar,
                    tipo = TipoBoton.PELIGRO,
                    habilitado = estado.puedeConfirmar,
                )
                BotonAncho(stringResource(R.string.accion_volver), onVolver, tipo = TipoBoton.SECUNDARIO)
            }
        }
    }
}

/** Qué pasará con el pago al confirmar (README: V7, P4 y P5), antes de que el Administrador lo confirme. */
@Composable
private fun EfectoEnElPago(estado: CancelarViajeUiState, tieneHash: Boolean) {
    val pago = estado.pago
    when {
        pago == null -> if (!tieneHash) Text(stringResource(R.string.aviso_sin_pago))
        pago.estado == EstadoPago.PAGADO && estado.pideReembolso ->
            Aviso(stringResource(R.string.cancelar_viaje_vencido), TonoAviso.INFORMACION)
        pago.estado == EstadoPago.PAGADO ->
            Aviso(stringResource(R.string.cancelar_viaje_reembolso_total, Formatos.monto(pago.monto)), TonoAviso.INFORMACION)
        pago.estado == EstadoPago.PENDIENTE -> Text(stringResource(R.string.cancelar_viaje_anula_pago))
        else -> Text(stringResource(R.string.cancelar_viaje_pago_sin_cambio), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, mono: Boolean = false) {
    Column {
        Text(etiqueta, style = EstilosMono.etiqueta)
        Text(valor, style = if (mono) EstilosMono.dato else MaterialTheme.typography.bodyLarge)
    }
}
