package cl.belmontespa.myapplication.ui.pantallas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.abordaje.AbordajeViewModel
import cl.belmontespa.myapplication.ui.administrador.AdministracionViewModel
import cl.belmontespa.myapplication.ui.administrador.AsignarScreen
import cl.belmontespa.myapplication.ui.administrador.SolicitudesScreen
import cl.belmontespa.myapplication.ui.auth.AccesoViewModel
import cl.belmontespa.myapplication.ui.auth.mensajeLogin
import cl.belmontespa.myapplication.ui.components.*
import cl.belmontespa.myapplication.ui.conductor.ConductorViewModel
import cl.belmontespa.myapplication.ui.conductor.ConfirmarInicioScreen
import cl.belmontespa.myapplication.ui.conductor.DetalleViajeScreen
import cl.belmontespa.myapplication.ui.conductor.MisViajesScreen
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.solicitante.SolicitanteViewModel
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import java.time.LocalDate
import java.time.LocalTime

// Un contenedor común mantiene espaciado, estados de carga y descarte consistentes.
@Composable
fun PantallaT06(
    pantalla: PantallaDemo,
    referencia: String = "",
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    raiz: Boolean = false,
) {
    // La variante de sesión vencida es el Login con su aviso, como muestran los mockups.
    if (pantalla in listOf(PantallaDemo.LOGIN, PantallaDemo.LOGIN_DEMO, PantallaDemo.SESION_EXPIRADA)) {
        Box(Modifier.fillMaxSize().testTag("t06_contenido")) {
            cl.belmontespa.myapplication.ui.auth.LoginScreen(
                onCrearCuenta = { onNavegar(PantallaDemo.REGISTRO, "") },
                onRecuperarPassword = { onNavegar(PantallaDemo.RECUPERAR, "") },
                onAbordarViaje = { onNavegar(PantallaDemo.ABORDAR, "") },
                correoInicial = referencia.takeIf { it.contains('@') }.orEmpty(),
                mostrarSesionExpirada = pantalla == PantallaDemo.SESION_EXPIRADA,
                modoDemoForzado = if (pantalla == PantallaDemo.LOGIN_DEMO) true else null,
            )
        }
        return
    }
    // «Solicitudes» del Administrador tiene su propia pantalla y su ViewModel (HU04).
    if (pantalla == PantallaDemo.SOLICITUDES) {
        SolicitudesScreen(onNavegar = onNavegar)
        return
    }
    // «Mis viajes» del Conductor tiene su pantalla y su ViewModel (HU07).
    if (pantalla == PantallaDemo.MIS_VIAJES) {
        MisViajesScreen(onNavegar = onNavegar)
        return
    }
    // El detalle del viaje del Conductor también (HU08); «Antes de la ventana» es la misma pantalla y
    // «Abrir navegación» (HU10) es la misma con la hoja «Abrir con» encima.
    if (pantalla == PantallaDemo.DETALLE_VIAJE || pantalla == PantallaDemo.ANTES_VENTANA || pantalla == PantallaDemo.NAVEGAR) {
        DetalleViajeScreen(folio = referencia, onNavegar = onNavegar, onVolver = onVolver, abrirNavegacion = pantalla == PantallaDemo.NAVEGAR)
        return
    }
    // «Confirmar inicio» del Conductor también (HU09): al iniciar vuelve al detalle.
    if (pantalla == PantallaDemo.CONFIRMAR_INICIO) {
        ConfirmarInicioScreen(folio = referencia, onVolver = onVolver)
        return
    }
    // «Asignar conductor y vehículo» también tiene su pantalla y su ViewModel (HU05, HU23).
    if (pantalla == PantallaDemo.ASIGNAR) {
        AsignarScreen(folio = referencia, onNavegar = onNavegar, onVolver = onVolver)
        return
    }
    val vm: PantallaBaseViewModel = when {
        pantalla.ordinal in 7..10 -> hiltViewModel<AbordajeViewModel>(key = pantalla.name + referencia)
        pantalla.rol == Rol.SOLICITANTE -> hiltViewModel<SolicitanteViewModel>(key = pantalla.name + referencia)
        pantalla.rol == Rol.ADMINISTRADOR -> hiltViewModel<AdministracionViewModel>(key = pantalla.name + referencia)
        pantalla.rol == Rol.CONDUCTOR -> hiltViewModel<ConductorViewModel>(key = pantalla.name + referencia)
        else -> hiltViewModel<AccesoViewModel>(key = pantalla.name + referencia)
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var confirmarSalida by remember { mutableStateOf(false) }
    val volver: () -> Unit = { if (estado.modificado) confirmarSalida = true else onVolver() }
    BackHandler(enabled = estado.modificado) { confirmarSalida = true }
    LaunchedEffect(pantalla, referencia) { vm.cargar(pantalla, referencia) }
    LaunchedEffect(estado.destino) {
        estado.destino?.let { (destino, id) -> vm.consumirDestino(); onNavegar(destino, id) }
    }
    if (confirmarSalida) AlertDialog(
        onDismissRequest = { confirmarSalida = false },
        text = { Text(stringResource(R.string.aviso_descartar)) },
        confirmButton = { TextButton(onClick = { confirmarSalida = false; onVolver() }) { Text(stringResource(R.string.accion_descartar)) } },
        dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text(stringResource(R.string.accion_seguir_editando)) } },
    )
    // Reserva el espacio del teclado para que los botones sigan siendo alcanzables.
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        BarraSuperior(titulo = stringResource(pantalla.titulo), onVolver = if (raiz) null else volver)
        if (estado.cargando) {
            PantallaCargando(modifier = Modifier.weight(1f))
        } else {
            Column(Modifier.testTag("t06_contenido").weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                estado.error?.let { error ->
                    val texto = when(error) {
                        is ErrorApp.Conflicto -> error.mensaje
                        is ErrorApp.Validacion -> error.errores.values.joinToString("\n")
                        else -> stringResource(mensajeLogin(error))
                    }
                    Aviso(texto, TonoAviso.PELIGRO, modifier = Modifier.testTag("t06_error"))
                    BotonAncho(stringResource(R.string.accion_reintentar), vm::actualizar, tipo = TipoBoton.SECUNDARIO)
                }
                estado.aviso?.let { Aviso(stringResource(it), TonoAviso.INFORMACION) }
                val contexto = ContextoPantalla(pantalla, referencia, estado, vm, onNavegar, volver, onCerrarSesion)
                ContenidoPantalla(contexto)
            }
        }
    }
}

internal data class ContextoPantalla(
    val pantalla: PantallaDemo,
    val id: String,
    val estado: EstadoPantallaDemo,
    val vm: PantallaBaseViewModel,
    val navegar: (PantallaDemo, String) -> Unit,
    val volver: () -> Unit,
    val cerrarSesion: () -> Unit,
)
internal data class Opcion(val id: String, val texto: String, val habilitada: Boolean = true)

@Composable
internal fun ContextoPantalla.Campo(clave: String, @StringRes etiqueta: Int, tipo: KeyboardType = KeyboardType.Text) {
    CampoTexto(estado.campos[clave].orEmpty(), { vm.campo(clave, it) }, stringResource(etiqueta),
        tipoTeclado = tipo, habilitado = !estado.enviando,
        error = (estado.error as? ErrorApp.Validacion)?.errores?.get(clave))
}
@Composable
internal fun ContextoPantalla.Accion(@StringRes texto: Int, accion: () -> Unit = { vm.ejecutar("guardar") }, tipo: TipoBoton = TipoBoton.PRINCIPAL, habilitada: Boolean = true, alto56: Boolean = false) {
    BotonAncho(stringResource(texto), accion, tipo = tipo, habilitado = habilitada && !estado.enviando, alto56 = alto56)
}
@Composable
internal fun ContextoPantalla.Enlace(@StringRes texto: Int, destino: PantallaDemo, id: String = this.id) {
    Accion(texto, { navegar(destino, id) }, TipoBoton.SECUNDARIO)
}
@Composable
internal fun ContextoPantalla.Selector(clave: String, @StringRes etiqueta: Int, opciones: List<Opcion>, alElegir: (() -> Unit)? = null) {
    var abierto by remember { mutableStateOf(false) }
    val valor = estado.campos[clave].orEmpty()
    val seleccionado = opciones.find { it.id == valor }?.texto ?: stringResource(R.string.aviso_selecciona)
    OutlinedButton(onClick = { abierto = true }, enabled = !estado.enviando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Column { Text(stringResource(etiqueta), style = EstilosMono.etiqueta); Text(seleccionado) }
    }
    if (abierto) AlertDialog(onDismissRequest = { abierto = false }, title = { Text(stringResource(etiqueta)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            opciones.forEach { opcion -> TextButton(onClick = { vm.campo(clave, opcion.id); abierto = false; alElegir?.invoke() }, enabled = opcion.habilitada, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(opcion.texto) } }
            if (opciones.isEmpty()) Text(stringResource(R.string.aviso_sin_resultados))
        } }, confirmButton = { TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.accion_cancelar)) } })
}
@Composable
internal fun Dato(@StringRes etiqueta: Int, valor: String?) {
    if (!valor.isNullOrBlank()) Column(Modifier.fillMaxWidth()) {
        Text(stringResource(etiqueta), style = EstilosMono.etiqueta)
        Text(valor, style = if (etiqueta in listOf(R.string.campo_folio, R.string.campo_patente, R.string.campo_monto, R.string.campo_fecha_agenda)) EstilosMono.dato else MaterialTheme.typography.bodyLarge)
    }
}
@Composable
internal fun Bloque(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            contenido()
        }
    }
}
@Composable
internal fun ContextoPantalla.FechaHora() {
    val contexto = LocalContext.current
    val fecha = runCatching { LocalDate.parse(estado.campos["fecha"]) }.getOrElse { vm.ahora().toLocalDate() }
    val hora = runCatching { LocalTime.parse(estado.campos["hora"]) }.getOrElse { LocalTime.of(12, 0) }
    Accion(R.string.campo_fecha, {
        DatePickerDialog(contexto, { _, y, m, d -> vm.campo("fecha", LocalDate.of(y, m + 1, d).toString()) }, fecha.year, fecha.monthValue - 1, fecha.dayOfMonth).show()
    }, TipoBoton.SECUNDARIO)
    Text(fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")), style = EstilosMono.dato)
    Accion(R.string.campo_hora, {
        TimePickerDialog(contexto, { _, h, m -> vm.campo("hora", LocalTime.of(h, m).toString()) }, hora.hour, hora.minute, true).show()
    }, TipoBoton.SECUNDARIO)
    Text(hora.toString(), style = EstilosMono.dato)
}
