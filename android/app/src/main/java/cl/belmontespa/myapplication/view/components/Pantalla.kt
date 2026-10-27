package cl.belmontespa.myapplication.view.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** Una opción de [SelectorOpciones]. */
data class Opcion(val id: String, val texto: String, val habilitada: Boolean = true)

/** El motivo que dejó la validación bajo [clave], si el error es de validación. */
fun ErrorApp?.deCampo(clave: String): String? = (this as? ErrorApp.Validacion)?.errores?.get(clave)

/**
 * Marco común de las pantallas de formulario, lista y detalle: barra superior, espacio del teclado, carga, error con
 * «Reintentar» (salvo en un formulario, donde reintentar no corrige nada) y confirmación al salir con cambios.
 * `onVolver` es `null` en las pantallas raíz de la barra inferior.
 */
@Composable
fun ContenedorPantalla(
    titulo: String,
    cargando: Boolean,
    modificado: Boolean,
    onVolver: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    error: ErrorApp? = null,
    aviso: Int? = null,
    onReintentar: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    var confirmarSalida by remember { mutableStateOf(false) }
    BackHandler(enabled = modificado) { confirmarSalida = true }
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            text = { Text(stringResource(R.string.aviso_descartar)) },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onVolver?.invoke() }) {
                    Text(stringResource(R.string.accion_descartar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) {
                    Text(stringResource(R.string.accion_seguir_editando))
                }
            },
        )
    }
    // Reserva el espacio del teclado para que los botones sigan siendo alcanzables.
    Column(modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        BarraSuperior(
            titulo = titulo,
            subtitulo = subtitulo,
            onVolver = onVolver?.let { volver -> { if (modificado) confirmarSalida = true else volver() } },
        )
        if (cargando) {
            PantallaCargando(modifier = Modifier.weight(1f))
        } else {
            Column(
                Modifier.testTag("pantalla_contenido").weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                error?.let { e ->
                    // Fuera del Login un 401 es una sesión terminada, no credenciales inválidas.
                    val texto = if (e is ErrorApp.Validacion) e.errores.values.joinToString("\n") else e.texto()
                    Aviso(texto, TonoAviso.PELIGRO, modifier = Modifier.testTag("pantalla_error"))
                    // Recargar no corrige un formulario; tras un 409 sí es la forma de leer el estado vigente.
                    if (e !is ErrorApp.Validacion && onReintentar != null) {
                        BotonAncho(stringResource(R.string.accion_reintentar), onReintentar, tipo = TipoBoton.SECUNDARIO)
                    }
                }
                aviso?.let { Aviso(stringResource(it), TonoAviso.INFORMACION) }
                contenido()
            }
        }
    }
}

/** Botón a ancho completo con su texto en `strings.xml`; se deshabilita mientras hay un envío en curso. */
@Composable
fun Accion(
    @StringRes texto: Int,
    onClick: () -> Unit,
    tipo: TipoBoton = TipoBoton.PRINCIPAL,
    habilitada: Boolean = true,
    enviando: Boolean = false,
    alto56: Boolean = false,
) {
    BotonAncho(stringResource(texto), onClick, tipo = tipo, habilitado = habilitada && !enviando, alto56 = alto56)
}

/** Campo de texto de un formulario: su error sale del `Validacion` del estado, bajo la [clave] del contrato. */
@Composable
fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    @StringRes etiqueta: Int,
    clave: String,
    error: ErrorApp?,
    enviando: Boolean,
    tipo: KeyboardType = KeyboardType.Text,
) {
    CampoTexto(valor, onCambio, stringResource(etiqueta), tipoTeclado = tipo, habilitado = !enviando, error = error.deCampo(clave))
}

/** Un dato de solo lectura con su etiqueta; no se dibuja si el valor está vacío. */
@Composable
fun Dato(@StringRes etiqueta: Int, valor: String?) {
    if (!valor.isNullOrBlank()) {
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(etiqueta), style = EstilosMono.etiqueta)
            val monoespaciado = etiqueta in listOf(
                R.string.campo_folio, R.string.campo_patente, R.string.campo_monto, R.string.campo_fecha_agenda,
            )
            Text(valor, style = if (monoespaciado) EstilosMono.dato else MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Tarjeta con un título (encabezado para TalkBack) y su contenido. */
@Composable
fun Bloque(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            contenido()
        }
    }
}

/** Botón que abre un diálogo con las [opciones]; muestra la elegida, o «Selecciona» si [valor] no está entre ellas. */
@Composable
fun SelectorOpciones(
    valor: String,
    @StringRes etiqueta: Int,
    opciones: List<Opcion>,
    onElegir: (String) -> Unit,
    habilitado: Boolean = true,
    error: String? = null,
) {
    var abierto by remember { mutableStateOf(false) }
    val seleccionado = opciones.find { it.id == valor }?.texto ?: stringResource(R.string.aviso_selecciona)
    OutlinedButton(
        onClick = { abierto = true },
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Column {
            Text(stringResource(etiqueta), style = EstilosMono.etiqueta)
            Text(seleccionado)
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (abierto) {
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(stringResource(etiqueta)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    opciones.forEach { opcion ->
                        TextButton(
                            onClick = { abierto = false; onElegir(opcion.id) },
                            enabled = opcion.habilitada,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text(opcion.texto) }
                    }
                    if (opciones.isEmpty()) Text(stringResource(R.string.aviso_sin_resultados))
                }
            },
            confirmButton = {
                TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

/** Botones «Fecha» y «Hora» con sus selectores del sistema, y el valor elegido debajo de cada uno. */
@Composable
fun CamposFechaHora(
    fecha: LocalDate,
    hora: LocalTime,
    onFecha: (LocalDate) -> Unit,
    onHora: (LocalTime) -> Unit,
    enviando: Boolean,
) {
    val contexto = LocalContext.current
    Accion(R.string.campo_fecha, {
        DatePickerDialog(
            contexto, { _, y, m, d -> onFecha(LocalDate.of(y, m + 1, d)) },
            fecha.year, fecha.monthValue - 1, fecha.dayOfMonth,
        ).show()
    }, TipoBoton.SECUNDARIO, enviando = enviando)
    Text(fecha.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), style = EstilosMono.dato)
    Accion(R.string.campo_hora, {
        TimePickerDialog(contexto, { _, h, m -> onHora(LocalTime.of(h, m)) }, hora.hour, hora.minute, true).show()
    }, TipoBoton.SECUNDARIO, enviando = enviando)
    Text(hora.toString(), style = EstilosMono.dato)
}

/** La hora del [leer] (el `Clock` del ViewModel), recalculada cada segundo para los chips Atrasado y Vencido. */
@Composable
fun rememberAhora(leer: () -> LocalDateTime): LocalDateTime {
    var ahora by remember { mutableStateOf(leer()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            ahora = leer()
        }
    }
    return ahora
}
