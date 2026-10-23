package cl.belmontespa.myapplication.ui.conductor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Arqueo
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.BarraSuperior
import cl.belmontespa.myapplication.ui.components.BotonAncho
import cl.belmontespa.myapplication.ui.components.CampoTexto
import cl.belmontespa.myapplication.ui.components.EstadoChip
import cl.belmontespa.myapplication.ui.components.EstiloChip
import cl.belmontespa.myapplication.ui.components.PantallaCargando
import cl.belmontespa.myapplication.ui.components.PantallaError
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.components.estiloChip
import cl.belmontespa.myapplication.ui.components.etiqueta
import cl.belmontespa.myapplication.ui.components.etiquetaCalculada
import cl.belmontespa.myapplication.ui.components.texto
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import cl.belmontespa.myapplication.util.Formatos

/**
 * Detalle del viaje del Conductor (HU08). «Antes de la ventana» no es otra pantalla: es este detalle con
 * Iniciar deshabilitado y la hora desde la que se habilita.
 */
@Composable
fun DetalleViajeScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: DetalleViajeViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    LaunchedEffect(estado.rechazado) { if (estado.rechazado) onNavegar(PantallaDemo.MIS_VIAJES, "") }
    // README (Retraso): la ventana de inicio y el retraso se recalculan al volver a primer plano.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alVolverAPrimerPlano() }
    DetalleViajeContenido(
        folio = folio,
        estado = estado,
        onActualizar = viewModel::actualizar,
        onVolver = onVolver,
        onNavegar = { onNavegar(PantallaDemo.NAVEGAR, folio) },
        onIniciar = { onNavegar(PantallaDemo.CONFIRMAR_INICIO, folio) },
        onFinalizar = viewModel::pedirFinalizar,
        onConfirmarFinalizar = viewModel::confirmarFinalizar,
        onDescartarFinalizar = viewModel::descartarFinalizar,
        onRechazar = viewModel::pedirRechazo,
        onMotivo = viewModel::escribirMotivo,
        onConfirmarRechazo = viewModel::confirmarRechazo,
        onDescartarRechazo = viewModel::descartarRechazo,
    )
}

@Composable
fun DetalleViajeContenido(
    folio: String,
    estado: DetalleViajeUiState,
    onActualizar: () -> Unit,
    onVolver: () -> Unit,
    onNavegar: () -> Unit,
    onIniciar: () -> Unit,
    onFinalizar: () -> Unit,
    onConfirmarFinalizar: () -> Unit,
    onDescartarFinalizar: () -> Unit,
    onRechazar: () -> Unit,
    onMotivo: (String) -> Unit,
    onConfirmarRechazo: () -> Unit,
    onDescartarRechazo: () -> Unit,
) {
    val viaje = estado.viaje
    val error = estado.error
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.detalle_viaje_titulo, folio), onVolver = onVolver)
        when {
            // Tras rechazar, el viaje deja de ser suyo: se espera la navegación en vez de mostrar «no encontrado».
            estado.cargando || estado.rechazado -> PantallaCargando()
            // Sin viaje no hay nada que mostrar: el error ocupa la pantalla.
            viaje == null -> Box(Modifier.testTag("t06_error")) {
                PantallaError(texto = (error ?: ErrorApp.NoEncontrado).texto(), onReintentar = onActualizar)
            }
            else -> Column(
                modifier = Modifier
                    .testTag("t06_contenido")
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (error != null) {
                    Aviso(error.texto(), TonoAviso.PELIGRO, Modifier.testTag("t06_error"))
                    // Un 409 ya recargó el viaje: reintentar solo tiene sentido si falló la lectura.
                    if (error !is ErrorApp.Conflicto) {
                        BotonAncho(stringResource(R.string.accion_reintentar), onActualizar, tipo = TipoBoton.SECUNDARIO)
                    }
                }
                Datos(estado)
                Abordaje(estado.arqueo, viaje.pasajeros)
                if (estado.conControles) {
                    Controles(estado, onNavegar, onIniciar, onFinalizar, onRechazar)
                }
                BotonAncho(stringResource(R.string.accion_actualizar), onActualizar, tipo = TipoBoton.SECUNDARIO)
            }
        }
    }
    if (estado.confirmandoFin) {
        AlertDialog(
            onDismissRequest = onDescartarFinalizar,
            text = { Text(stringResource(R.string.aviso_finalizar)) },
            confirmButton = { TextButton(onClick = onConfirmarFinalizar) { Text(stringResource(R.string.accion_confirmar)) } },
            dismissButton = { TextButton(onClick = onDescartarFinalizar) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
    if (estado.rechazando) {
        AlertDialog(
            onDismissRequest = onDescartarRechazo,
            title = { Text(stringResource(R.string.accion_rechazar)) },
            text = { CampoTexto(valor = estado.motivo, onCambio = onMotivo, etiqueta = stringResource(R.string.campo_motivo)) },
            confirmButton = {
                TextButton(onClick = onConfirmarRechazo, enabled = estado.puedeConfirmarRechazo) {
                    Text(stringResource(R.string.accion_rechazar))
                }
            },
            dismissButton = { TextButton(onClick = onDescartarRechazo) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Datos(estado: DetalleViajeUiState) {
    val viaje = estado.viaje ?: return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip, grande = true)
        estado.retraso?.etiquetaCalculada?.let { EstadoChip(stringResource(it.etiqueta), it.estiloChip, grande = true) }
    }
    Dato(stringResource(R.string.detalle_viaje_horario), Formatos.fechaHoraDetalle(viaje.fhAgenda), mono = true)
    viaje.numeroVuelo?.let { Dato(stringResource(R.string.detalle_viaje_vuelo), it, mono = true) }
    Dato(stringResource(R.string.detalle_viaje_solicitante), viaje.nombreSolicitante)
    viaje.telefonoSolicitante?.let { Dato(stringResource(R.string.campo_telefono), Formatos.telefono(it), mono = true) }
    Dato(stringResource(R.string.campo_origen), viaje.origen.direccion)
    Dato(stringResource(R.string.campo_destino), viaje.destino.direccion)
    viaje.patenteMovil?.let { Dato(stringResource(R.string.campo_vehiculo), it, mono = true) }
    viaje.motivo?.let { Dato(stringResource(R.string.campo_motivo), it) }
}

@Composable
private fun Dato(etiqueta: String, valor: String, mono: Boolean = false) {
    Column {
        Text(etiqueta, style = EstilosMono.etiqueta)
        Text(valor, style = if (mono) EstilosMono.dato else MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Abordaje(arqueo: Arqueo?, pasajeros: List<PasajeroViaje>) {
    if (arqueo == null) return
    Text(
        stringResource(R.string.campo_abordaje),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    // Un solo elemento permite que TalkBack anuncie el arqueo completo (README, Accesibilidad).
    val texto = if (arqueo.completo) {
        stringResource(R.string.arqueo_todos)
    } else {
        stringResource(R.string.arqueo_faltan, arqueo.abordaron, arqueo.total, arqueo.faltan.joinToString())
    }
    Aviso(texto, if (arqueo.completo) TonoAviso.INFORMACION else TonoAviso.AVISO)
    pasajeros.forEach { FilaPasajero(it) }
}

@Composable
private fun FilaPasajero(pasajero: PasajeroViaje) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pasajero.nombre, style = MaterialTheme.typography.titleMedium)
                pasajero.fhAbordaje?.let {
                    Text(
                        stringResource(R.string.detalle_viaje_abordo_a_las, Formatos.hora(it)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (pasajero.fhAbordaje != null) {
                EstadoChip(stringResource(R.string.etiqueta_abordado), EstiloChip.CORRECTO)
            } else {
                EstadoChip(stringResource(R.string.etiqueta_por_abordar), EstiloChip.NEUTRO)
            }
        }
    }
}

/** Iniciar y Finalizar van juntos; solo el aplicable está habilitado. Los demás botones, solo si aplican. */
@Composable
private fun Controles(
    estado: DetalleViajeUiState,
    onNavegar: () -> Unit,
    onIniciar: () -> Unit,
    onFinalizar: () -> Unit,
    onRechazar: () -> Unit,
) {
    val viaje = estado.viaje ?: return
    BotonAncho(stringResource(R.string.accion_iniciar), onIniciar, habilitado = estado.puedeIniciar, alto56 = true)
    BotonAncho(
        stringResource(R.string.accion_finalizar), onFinalizar,
        tipo = TipoBoton.SECUNDARIO, habilitado = estado.puedeFinalizar, alto56 = true,
    )
    estado.disponibleDesde?.let {
        Text(
            stringResource(R.string.iniciar_disponible_desde, Formatos.hora(it)),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (estado.inicio == Inicio.VENCIDO) {
        Aviso(stringResource(R.string.iniciar_vencido), TonoAviso.AVISO)
    }
    val navegar = if (viaje.estado == EstadoViaje.EN_CURSO) R.string.accion_navegar_destino else R.string.accion_navegar_origen
    BotonAncho(stringResource(navegar), onNavegar, tipo = TipoBoton.SECUNDARIO, alto56 = true)
    if (viaje.estado == EstadoViaje.ASIGNADO) {
        BotonAncho(
            stringResource(R.string.accion_rechazar), onRechazar,
            tipo = TipoBoton.PELIGRO, habilitado = estado.puedeRechazar, alto56 = true,
        )
    }
}
