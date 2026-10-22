package cl.belmontespa.myapplication.ui.administrador

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import cl.belmontespa.myapplication.model.EstadoViaje
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

/**
 * «Solicitudes» del Administrador (HU04): los viajes de todos los Solicitantes, con el filtro inicial
 * «Pendientes». En las pendientes que no están vencidas y tienen pasajeros, «Asignar» abre directo la
 * asignación.
 */
@Composable
fun SolicitudesScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    viewModel: SolicitudesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // README (Retraso): «Atrasado» y «Vencido» se recalculan al volver a primer plano.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alVolverAPrimerPlano() }
    SolicitudesContenido(
        estado = estado,
        onFiltro = viewModel::elegirFiltro,
        onActualizar = viewModel::actualizar,
        onCuentasPorAprobar = { onNavegar(PantallaDemo.CUENTAS_PENDIENTES, "") },
        onVerDetalle = { viaje ->
            val destino = if (viaje.cantidadPasajeros == 0) PantallaDemo.SIN_PASAJEROS_ADMIN else PantallaDemo.DETALLE_ADMIN
            onNavegar(destino, viaje.folio)
        },
        onAsignar = { viaje -> onNavegar(PantallaDemo.ASIGNAR, viaje.folio) },
    )
}

@Composable
fun SolicitudesContenido(
    estado: SolicitudesUiState,
    onFiltro: (FiltroSolicitudes) -> Unit,
    onActualizar: () -> Unit,
    onCuentasPorAprobar: () -> Unit,
    onVerDetalle: (Viaje) -> Unit,
    onAsignar: (Viaje) -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.t06_solicitudes))
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
                item {
                    BotonAncho(
                        texto = stringResource(R.string.cuentas_pendientes_cantidad, estado.cuentasPorAprobar),
                        onClick = onCuentasPorAprobar,
                        tipo = TipoBoton.SECUNDARIO,
                    )
                }
                item { Filtros(seleccionado = estado.filtro, onFiltro = onFiltro) }
                if (error != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Aviso(error.texto(), TonoAviso.PELIGRO, Modifier.testTag("t06_error"))
                            BotonAncho(stringResource(R.string.accion_reintentar), onActualizar, tipo = TipoBoton.SECUNDARIO)
                        }
                    }
                }
                if (estado.filas.isEmpty()) {
                    item { Text(stringResource(textoVacio(estado.filtro)), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(estado.filas, key = { it.viaje.folio }) { fila ->
                    FilaSolicitud(fila, onVerDetalle = { onVerDetalle(fila.viaje) }, onAsignar = { onAsignar(fila.viaje) })
                }
                // Alternativa al gesto de deslizar para actualizar.
                item { BotonAncho(stringResource(R.string.accion_actualizar), onActualizar, tipo = TipoBoton.SECUNDARIO) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filtros(seleccionado: FiltroSolicitudes, onFiltro: (FiltroSolicitudes) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FiltroSolicitudes.entries.forEach { filtro ->
            FilterChip(
                selected = filtro == seleccionado,
                onClick = { onFiltro(filtro) },
                label = { Text(stringResource(filtro.etiqueta)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaSolicitud(fila: SolicitudFila, onVerDetalle: () -> Unit, onAsignar: () -> Unit) {
    val viaje = fila.viaje
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.accion_ver_detalle), onClick = onVerDetalle)
                .heightIn(min = 48.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(viaje.folio, style = EstilosMono.dato)
            Text(Formatos.fechaHoraLista(viaje.fhAgenda), style = EstilosMono.dato)
            Text(viaje.nombreSolicitante, style = MaterialTheme.typography.titleMedium)
            Text("${viaje.origen.direccion} → ${viaje.destino.direccion}")
            Text(
                pluralStringResource(R.plurals.solicitud_cantidad_pasajeros, viaje.cantidadPasajeros, viaje.cantidadPasajeros),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip)
                fila.retraso?.etiquetaCalculada?.let { EstadoChip(stringResource(it.etiqueta), it.estiloChip) }
            }
        }
        if (fila.puedeAsignar) {
            BotonAncho(
                texto = stringResource(R.string.accion_asignar),
                onClick = onAsignar,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                tipo = TipoBoton.SECUNDARIO,
            )
        }
    }
}

// Los filtros usan las etiquetas de estado; «Pendientes» y «Todas» llevan el plural de los mockups.
@get:StringRes
private val FiltroSolicitudes.etiqueta: Int
    get() = when (this) {
        FiltroSolicitudes.PENDIENTES -> R.string.filtro_pendientes
        FiltroSolicitudes.ASIGNADO -> EstadoViaje.ASIGNADO.etiqueta
        FiltroSolicitudes.EN_CURSO -> EstadoViaje.EN_CURSO.etiqueta
        FiltroSolicitudes.FINALIZADO -> EstadoViaje.FINALIZADO.etiqueta
        FiltroSolicitudes.CANCELADO -> EstadoViaje.CANCELADO.etiqueta
        FiltroSolicitudes.TODAS -> R.string.filtro_todas
    }

@StringRes
private fun textoVacio(filtro: FiltroSolicitudes): Int =
    if (filtro == FiltroSolicitudes.PENDIENTES) R.string.vacio_solicitudes_pendientes else R.string.aviso_sin_resultados
