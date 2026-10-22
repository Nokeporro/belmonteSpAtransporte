package cl.belmontespa.myapplication.ui.conductor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.BarraSuperior
import cl.belmontespa.myapplication.ui.components.BotonAncho
import cl.belmontespa.myapplication.ui.components.EstadoChip
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * «Mis viajes» del Conductor (HU07): su agenda del día por hora. Tocar un viaje abre su detalle; antes de
 * la ventana de inicio, la variante «Antes de la ventana».
 */
@Composable
fun MisViajesScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    viewModel: MisViajesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // README (Retraso): «Atrasado» y «Vencido» se recalculan al volver a primer plano.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alVolverAPrimerPlano() }
    MisViajesContenido(
        estado = estado,
        onFecha = viewModel::elegirFecha,
        onActualizar = viewModel::actualizar,
        onVerDetalle = { fila ->
            val destino = if (fila.antesDeLaVentana) PantallaDemo.ANTES_VENTANA else PantallaDemo.DETALLE_VIAJE
            onNavegar(destino, fila.viaje.folio)
        },
    )
}

@Composable
fun MisViajesContenido(
    estado: MisViajesUiState,
    onFecha: (LocalDate) -> Unit,
    onActualizar: () -> Unit,
    onVerDetalle: (ViajeFila) -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.t06_mis_viajes), subtitulo = estado.conductor)
        val error = estado.error
        when {
            estado.cargando -> PantallaCargando()
            // Sin nada que mostrar, el error ocupa la pantalla; con filas, avisa sobre la lista.
            error != null && estado.filas.isEmpty() -> Box(Modifier.testTag("t06_error")) {
                PantallaError(texto = error.texto(), onReintentar = onActualizar)
            }
            else -> LazyColumn(
                modifier = Modifier.testTag("t06_contenido").fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SelectorDeFecha(fecha = estado.fecha, esHoy = estado.esHoy, onFecha = onFecha) }
                if (error != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Aviso(error.texto(), TonoAviso.PELIGRO, Modifier.testTag("t06_error"))
                            BotonAncho(stringResource(R.string.accion_reintentar), onActualizar, tipo = TipoBoton.SECUNDARIO)
                        }
                    }
                }
                if (estado.filas.isEmpty()) {
                    item { Text(stringResource(R.string.vacio_mis_viajes), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(estado.filas, key = { it.viaje.folio }) { fila ->
                    FilaViaje(fila, dia = estado.fecha, onVerDetalle = { onVerDetalle(fila) })
                }
                // Alternativa al gesto de deslizar para actualizar.
                item { BotonAncho(stringResource(R.string.accion_actualizar), onActualizar, tipo = TipoBoton.SECUNDARIO) }
            }
        }
    }
}

/** La fecha del selector de Material 3 viaja como milisegundos UTC a medianoche del día elegido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeFecha(fecha: LocalDate, esHoy: Boolean, onFecha: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val texto = Formatos.fecha(fecha).let { if (esHoy) stringResource(R.string.mis_viajes_hoy, it) else it }
    OutlinedButton(
        onClick = { abierto = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Column {
            Text(stringResource(R.string.campo_fecha), style = EstilosMono.etiqueta)
            Text(texto, style = EstilosMono.dato)
        }
    }
    if (abierto) {
        val selector = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selector.selectedDateMillis?.let {
                            onFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        abierto = false
                    },
                ) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        ) { DatePicker(state = selector) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaViaje(fila: ViajeFila, dia: LocalDate, onVerDetalle: () -> Unit) {
    val viaje = fila.viaje
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.accion_ver_detalle), onClick = onVerDetalle)
                .heightIn(min = 48.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // El EN_CURSO de un día anterior entra en la agenda de hoy: con solo la hora parecería de hoy.
            val hora = if (viaje.fhAgenda.toLocalDate() == dia) Formatos.hora(viaje.fhAgenda) else Formatos.fechaHoraLista(viaje.fhAgenda)
            Text(hora, style = EstilosMono.dato)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip)
                    fila.retraso?.etiquetaCalculada?.let { EstadoChip(stringResource(it.etiqueta), it.estiloChip) }
                }
                Text(pasajeros(viaje), style = MaterialTheme.typography.titleMedium)
                Text("${viaje.origen.direccion} → ${viaje.destino.direccion}")
            }
        }
    }
}

// «Constanza Ríos +1»: el primer pasajero (por nombre) y cuántos más van.
@Composable
private fun pasajeros(viaje: Viaje): String {
    val primero = viaje.pasajeros.firstOrNull()?.nombre
        ?: return pluralStringResource(R.plurals.solicitud_cantidad_pasajeros, viaje.cantidadPasajeros, viaje.cantidadPasajeros)
    val otros = viaje.cantidadPasajeros - 1
    return if (otros > 0) stringResource(R.string.mis_viajes_pasajero_y_otros, primero, otros) else primero
}
