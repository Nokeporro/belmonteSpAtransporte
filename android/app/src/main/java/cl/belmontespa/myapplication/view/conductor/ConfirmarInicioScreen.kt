package cl.belmontespa.myapplication.view.conductor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.disponibleDesde
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.BarraSuperior
import cl.belmontespa.myapplication.view.components.BotonAncho
import cl.belmontespa.myapplication.view.components.PantallaCargando
import cl.belmontespa.myapplication.view.components.PantallaError
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.view.components.texto
import cl.belmontespa.myapplication.viewmodel.conductor.ConfirmarInicioUiState
import cl.belmontespa.myapplication.viewmodel.conductor.ConfirmarInicioViewModel

/**
 * «Confirmar inicio» (HU09): el Conductor confirma que pasa el viaje a En curso. Volver deja el viaje como está.
 * Al iniciarse vuelve al detalle, que ya muestra el estado nuevo porque lee el mismo viaje de Room.
 */
@Composable
fun ConfirmarInicioScreen(
    folio: String,
    onVolver: () -> Unit,
    viewModel: ConfirmarInicioViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    LaunchedEffect(estado.iniciado) { if (estado.iniciado) onVolver() }
    // README (Retraso): la ventana de inicio se recalcula al volver a primer plano.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alVolverAPrimerPlano() }
    ConfirmarInicioContenido(
        folio = folio,
        estado = estado,
        onActualizar = viewModel::actualizar,
        onVolver = onVolver,
        onIniciar = viewModel::confirmar,
    )
}

@Composable
fun ConfirmarInicioContenido(
    folio: String,
    estado: ConfirmarInicioUiState,
    onActualizar: () -> Unit,
    onVolver: () -> Unit,
    onIniciar: () -> Unit,
) {
    val viaje = estado.viaje
    val error = estado.error
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.detalle_viaje_titulo, folio), onVolver = onVolver)
        when {
            // Tras iniciar se espera la navegación al detalle en vez de mostrar el botón otra vez.
            estado.cargando || estado.iniciado -> PantallaCargando()
            // Sin viaje no hay nada que confirmar: el error ocupa la pantalla.
            viaje == null -> Box(Modifier.testTag("pantalla_error")) {
                PantallaError(texto = (error ?: ErrorApp.NoEncontrado).texto(), onReintentar = onActualizar)
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
                if (error != null) Aviso(error.texto(), TonoAviso.PELIGRO, Modifier.testTag("pantalla_error"))
                Text(
                    stringResource(R.string.confirmar_inicio_titulo, viaje.folio),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                viaje.patenteMovil?.let { Text(stringResource(R.string.confirmar_inicio_detalle, it)) }
                if (estado.faltan > 0) {
                    Aviso(pluralStringResource(R.plurals.faltan_por_abordar, estado.faltan, estado.faltan), TonoAviso.AVISO)
                }
                estado.disponibleDesde?.let {
                    Text(
                        stringResource(R.string.iniciar_disponible_desde, Formatos.hora(it)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (estado.inicio == Inicio.VENCIDO) Aviso(stringResource(R.string.iniciar_vencido), TonoAviso.AVISO)
                BotonAncho(stringResource(R.string.accion_iniciar), onIniciar, habilitado = estado.puedeIniciar, alto56 = true)
                BotonAncho(stringResource(R.string.accion_volver), onVolver, tipo = TipoBoton.SECUNDARIO, alto56 = true)
            }
        }
    }
}
