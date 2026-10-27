package cl.belmontespa.myapplication.ui.administrador

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
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
import cl.belmontespa.myapplication.ui.components.texto
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.EstilosMono

/**
 * «Asignar conductor y vehículo» (HU05); con el viaje `ASIGNADO` es la reasignación (HU23). Elegir un
 * conductor muestra sus vehículos habilitados; «Confirmar asignación» guarda y vuelve al detalle.
 */
@Composable
fun AsignarScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: AsignarViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var confirmarSalida by remember { mutableStateOf(false) }
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    LaunchedEffect(estado.asignado) { if (estado.asignado) onNavegar(PantallaDemo.DETALLE_ADMIN, folio) }
    // Con una elección a medias, salir pide confirmar el descarte.
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
    AsignarContenido(
        estado = estado,
        onConductor = viewModel::elegirConductor,
        onVehiculo = viewModel::elegirVehiculo,
        onConfirmar = viewModel::confirmar,
        onReintentar = viewModel::reintentar,
        onVolver = { if (estado.modificado) confirmarSalida = true else onVolver() },
    )
}

@Composable
fun AsignarContenido(
    estado: AsignarUiState,
    onConductor: (String) -> Unit,
    onVehiculo: (String) -> Unit,
    onConfirmar: () -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
) {
    val viaje = estado.viaje
    val error = estado.error
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(
            titulo = stringResource(R.string.t06_asignar),
            subtitulo = viaje?.let {
                "${it.folio} · " + pluralStringResource(R.plurals.solicitud_cantidad_pasajeros, it.cantidadPasajeros, it.cantidadPasajeros)
            },
            onVolver = onVolver,
        )
        when {
            estado.cargando -> PantallaCargando()
            // Sin viaje no hay nada que asignar: el error ocupa la pantalla.
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
                    // Un 409 ya recargó los datos: reintentar solo tiene sentido si falló la lectura.
                    if (error !is ErrorApp.Conflicto) {
                        BotonAncho(stringResource(R.string.accion_reintentar), onReintentar, tipo = TipoBoton.SECUNDARIO)
                    }
                }
                if (estado.noAsignable != null) {
                    val texto = if (estado.noAsignable == NoAsignable.SIN_PASAJEROS) R.string.aviso_sin_pasajeros_asignar else R.string.aviso_viaje_no_asignable
                    Aviso(stringResource(texto), TonoAviso.AVISO)
                } else {
                    Formulario(estado, onConductor, onVehiculo, onConfirmar)
                }
            }
        }
    }
}

@Composable
private fun Formulario(
    estado: AsignarUiState,
    onConductor: (String) -> Unit,
    onVehiculo: (String) -> Unit,
    onConfirmar: () -> Unit,
) {
    Seccion(stringResource(R.string.campo_conductor)) {
        if (estado.conductores.none { it.habilitado }) {
            Text(stringResource(R.string.vacio_conductores_disponibles), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        estado.conductores.forEach { conductor ->
            FilaConductor(conductor, seleccionado = conductor.correo == estado.conductorElegido, onElegir = { onConductor(conductor.correo) })
        }
    }
    val conductor = estado.conductor
    if (conductor != null) {
        Seccion(stringResource(R.string.asignar_vehiculo_habilitado_para, conductor.nombre)) {
            when {
                estado.cargandoVehiculos -> CircularProgressIndicator(Modifier.padding(8.dp))
                estado.vehiculos.isEmpty() -> Text(stringResource(R.string.asignar_sin_vehiculos), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> estado.vehiculos.forEach { vehiculo ->
                    FilaVehiculo(vehiculo, seleccionado = vehiculo.patente == estado.vehiculoElegido, onElegir = { onVehiculo(vehiculo.patente) })
                }
            }
        }
    }
    val vehiculo = estado.vehiculo
    if (conductor != null && vehiculo != null) {
        Seccion(stringResource(R.string.asignar_resumen)) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dato(stringResource(R.string.campo_conductor), conductor.nombre)
                    Dato(stringResource(R.string.campo_vehiculo), vehiculo.patente, mono = true)
                }
            }
        }
    }
    BotonAncho(
        texto = stringResource(R.string.accion_confirmar_asignacion),
        onClick = onConfirmar,
        habilitado = estado.puedeConfirmar,
    )
    Text(stringResource(R.string.asignar_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        contenido()
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, mono: Boolean = false) {
    Column {
        Text(etiqueta, style = EstilosMono.etiqueta)
        Text(valor, style = if (mono) EstilosMono.dato else MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FilaConductor(conductor: ConductorOpcion, seleccionado: Boolean, onElegir: () -> Unit) {
    OpcionSeleccionable(seleccionado, conductor.habilitado, onElegir) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(conductor.nombre, style = MaterialTheme.typography.titleMedium)
            val detalle = when {
                conductor.folioEnCurso != null -> stringResource(R.string.asignar_en_curso, conductor.folioEnCurso)
                conductor.viajesAsignados > 0 ->
                    pluralStringResource(R.plurals.asignar_viajes_asignados, conductor.viajesAsignados, conductor.viajesAsignados)
                else -> stringResource(R.string.asignar_sin_viajes)
            }
            Text(detalle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            EstadoChip(stringResource(conductor.estado.etiqueta), conductor.estado.estiloChip)
        }
    }
}

@Composable
private fun FilaVehiculo(vehiculo: VehiculoOpcion, seleccionado: Boolean, onElegir: () -> Unit) {
    OpcionSeleccionable(seleccionado, habilitada = true, onElegir) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(vehiculo.patente, style = EstilosMono.dato)
            Text(
                pluralStringResource(R.plurals.solicitud_cantidad_pasajeros, vehiculo.capacidad, vehiculo.capacidad),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EstadoChip(stringResource(vehiculo.estado.etiqueta), vehiculo.estado.estiloChip)
        }
    }
}

/** Fila de una opción única: toda la tarjeta se toca (48 dp como mínimo) y el estado lo lee TalkBack. */
@Composable
private fun OpcionSeleccionable(
    seleccionada: Boolean,
    habilitada: Boolean,
    onElegir: () -> Unit,
    contenido: @Composable RowScope.() -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth().alpha(if (habilitada) 1f else 0.6f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .selectable(selected = seleccionada, enabled = habilitada, role = Role.RadioButton, onClick = onElegir)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = seleccionada, onClick = null, enabled = habilitada)
            contenido()
        }
    }
}
