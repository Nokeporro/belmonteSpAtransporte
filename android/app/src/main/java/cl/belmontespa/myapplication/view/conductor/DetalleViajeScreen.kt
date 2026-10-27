package cl.belmontespa.myapplication.view.conductor

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.arqueo
import cl.belmontespa.myapplication.model.disponibleDesde
import cl.belmontespa.myapplication.model.retraso
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.ArqueoDeAbordaje
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.BannerSinConexion
import cl.belmontespa.myapplication.view.components.BarraSuperior
import cl.belmontespa.myapplication.view.components.BotonAncho
import cl.belmontespa.myapplication.view.components.CampoTexto
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.EstiloChip
import cl.belmontespa.myapplication.view.components.PantallaCargando
import cl.belmontespa.myapplication.view.components.PantallaError
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.view.components.etiquetaCalculada
import cl.belmontespa.myapplication.view.components.texto
import cl.belmontespa.myapplication.viewmodel.administrador.CAMPO_MOTIVO
import cl.belmontespa.myapplication.viewmodel.conductor.DetalleViajeUiState
import cl.belmontespa.myapplication.viewmodel.conductor.DetalleViajeViewModel

/**
 * Detalle del viaje del Conductor (HU08). «Antes de la ventana» no es otra pantalla: es este detalle con
 * Iniciar deshabilitado y la hora desde la que se habilita. «Abrir navegación» (HU10) tampoco: es este detalle con
 * la hoja «Abrir con» encima; `abrirNavegacion` la muestra de entrada (sin abrir ninguna aplicación por su cuenta).
 */
@Composable
fun DetalleViajeScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    abrirNavegacion: Boolean = false,
    escenario: EscenarioDemo? = null,
    viewModel: DetalleViajeViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    // La hoja necesita el viaje (de él sale el punto de la ruta): se espera a que cargue.
    val cargado = estado.viaje != null
    LaunchedEffect(abrirNavegacion, cargado) { if (abrirNavegacion && cargado) viewModel.mostrarOpciones() }
    LaunchedEffect(estado.rechazado) { if (estado.rechazado) onNavegar(PantallaDemo.MIS_VIAJES, "") }
    // README (Retraso): la ventana de inicio y el retraso se recalculan al volver a primer plano.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alVolverAPrimerPlano() }
    DetalleViajeContenido(
        folio = folio,
        estado = estado,
        onActualizar = viewModel::actualizar,
        onVolver = onVolver,
        onNavegar = viewModel::navegar,
        onIniciar = { onNavegar(PantallaDemo.CONFIRMAR_INICIO, folio) },
        onFinalizar = viewModel::pedirFinalizar,
        onConfirmarFinalizar = viewModel::confirmarFinalizar,
        onDescartarFinalizar = viewModel::descartarFinalizar,
        onRechazar = viewModel::pedirRechazo,
        onMotivo = viewModel::escribirMotivo,
        onConfirmarRechazo = viewModel::confirmarRechazo,
        onDescartarRechazo = viewModel::descartarRechazo,
        onElegirApp = viewModel::elegir,
        onCerrarOpciones = viewModel::cerrarOpciones,
        escenario = escenario,
        onDescartarCambio = { onNavegar(PantallaDemo.DETALLE_VIAJE, folio) },
    )
}

/**
 * Escenarios de la build `demo` que se ven sobre el detalle del Conductor (README, Sesión y conectividad); en la
 * remota los produce la cola sin conexión.
 */
enum class EscenarioDemo { SIN_CONEXION, CONFLICTO }

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
    onElegirApp: (AppNavegacion) -> Unit,
    onCerrarOpciones: () -> Unit,
    escenario: EscenarioDemo? = null,
    onDescartarCambio: () -> Unit = {},
) {
    val viaje = estado.viaje
    val error = estado.error
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.detalle_viaje_titulo, folio), onVolver = onVolver)
        when {
            // Tras rechazar, el viaje deja de ser suyo: se espera la navegación en vez de mostrar «no encontrado».
            estado.cargando || estado.rechazado -> PantallaCargando()
            // Sin viaje no hay nada que mostrar: el error ocupa la pantalla.
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
                if (error != null) {
                    Aviso(error.texto(), TonoAviso.PELIGRO, Modifier.testTag("pantalla_error"))
                    // Un 409 ya recargó el viaje: reintentar solo tiene sentido si falló la lectura.
                    if (error !is ErrorApp.Conflicto) {
                        BotonAncho(stringResource(R.string.accion_reintentar), onActualizar, tipo = TipoBoton.SECUNDARIO)
                    }
                }
                Escenario(escenario, onActualizar, onDescartarCambio)
                Datos(estado)
                estado.arqueo?.let { ArqueoDeAbordaje(it, viaje.pasajeros) }
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
    estado.opcionesDeNavegacion?.let { HojaAbrirCon(it, onElegirApp, onCerrarOpciones) }
    if (estado.rechazando) {
        AlertDialog(
            onDismissRequest = onDescartarRechazo,
            title = { Text(stringResource(R.string.accion_rechazar)) },
            text = {
                CampoTexto(
                    valor = estado.motivo,
                    onCambio = onMotivo,
                    etiqueta = stringResource(R.string.campo_motivo),
                    error = estado.errores[CAMPO_MOTIVO],
                    habilitado = !estado.enviando,
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmarRechazo, enabled = estado.puedeConfirmarRechazo) {
                    Text(stringResource(R.string.accion_rechazar))
                }
            },
            dismissButton = { TextButton(onClick = onDescartarRechazo) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}

/** Los avisos del escenario «Sin conexión» o «Cambio en conflicto», sobre el detalle. */
@Composable
private fun Escenario(escenario: EscenarioDemo?, onActualizar: () -> Unit, onDescartarCambio: () -> Unit) {
    when (escenario) {
        EscenarioDemo.SIN_CONEXION -> {
            BannerSinConexion()
            EstadoChip(stringResource(R.string.etiqueta_pendiente_sincronizar), EstiloChip.AVISO)
            Aviso(stringResource(R.string.aviso_sincronizacion_demo), TonoAviso.INFORMACION)
        }
        EscenarioDemo.CONFLICTO -> {
            Aviso(stringResource(R.string.aviso_escenario_conflicto), TonoAviso.INFORMACION)
            BotonAncho(stringResource(R.string.accion_descartar_cambio), onDescartarCambio, tipo = TipoBoton.SECUNDARIO)
            Aviso(stringResource(R.string.aviso_cambio_conflicto), TonoAviso.PELIGRO)
            BotonAncho(stringResource(R.string.accion_actualizar), onActualizar, tipo = TipoBoton.SECUNDARIO)
        }
        null -> Unit
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
    // Etiqueta propia: no es un fallo de la pantalla, así que no lleva `t06_error`.
    if (estado.sinAppDeNavegacion) {
        Aviso(stringResource(R.string.error_sin_app_navegacion), TonoAviso.AVISO, Modifier.testTag("pantalla_aviso_navegacion"))
    }
    if (viaje.estado == EstadoViaje.ASIGNADO) {
        BotonAncho(
            stringResource(R.string.accion_rechazar), onRechazar,
            tipo = TipoBoton.PELIGRO, habilitado = estado.puedeRechazar, alto56 = true,
        )
    }
}

/** Hoja «Abrir con» (mockup «Abrir navegación»): solo las aplicaciones instaladas, cada una con área táctil de 56 dp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaAbrirCon(apps: List<AppNavegacion>, onElegir: (AppNavegacion) -> Unit, onCerrar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onCerrar, modifier = Modifier.testTag("pantalla_hoja_navegacion")) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.navegacion_abrir_con),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
            )
            apps.forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) { onElegir(app) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.nombre, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
