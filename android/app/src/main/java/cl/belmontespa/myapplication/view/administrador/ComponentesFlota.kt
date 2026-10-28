package cl.belmontespa.myapplication.view.administrador

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta

@Composable
internal fun ResumenAgendaConductor(conductor: Conductor) {
    Text(pluralStringResource(R.plurals.asignar_viajes_asignados, conductor.viajesAsignados, conductor.viajesAsignados))
    conductor.folioEnCurso?.let { Text(stringResource(R.string.asignar_en_curso, it), style = EstilosMono.dato) }
}

@Composable
internal fun ResumenHabilitaciones(habilitaciones: List<Habilitacion>) {
    habilitaciones.forEach { h ->
        Bloque(h.patente) {
            Dato(R.string.campo_conductor, h.nombreConductor)
            Dato(R.string.campo_correo, h.correoConductor)
            Dato(R.string.campo_capacidad, h.capacidad.toString())
            Text(stringResource(R.string.habilitaciones_estado_vehiculo))
            EstadoChip(stringResource(h.estadoMovil.etiqueta(h.activoMovil)), h.estadoMovil.estiloChip(h.activoMovil))
            Text(stringResource(R.string.habilitaciones_estado_conductor))
            EstadoChip(stringResource(h.estadoConductor.etiqueta(h.activoConductor)), h.estadoConductor.estiloChip(h.activoConductor))
        }
    }
}

@Composable
internal fun CambioActivo(
    identificador: String,
    activo: Boolean,
    habilitado: Boolean,
    enviando: Boolean,
    onConfirmar: () -> Unit,
) {
    var confirmar by remember(identificador, activo) { mutableStateOf(false) }
    val etiqueta = if (activo) R.string.accion_desactivar else R.string.accion_reactivar
    Text(stringResource(R.string.flota_activo_ayuda))
    Accion(etiqueta, { confirmar = true }, TipoBoton.SECUNDARIO, habilitada = habilitado, enviando = enviando)
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text(stringResource(etiqueta)) },
            text = {
                Text(stringResource(if (activo) R.string.flota_confirmar_desactivar else R.string.flota_confirmar_reactivar, identificador))
            },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onConfirmar() }, enabled = !enviando && habilitado) {
                    Text(stringResource(R.string.accion_confirmar))
                }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}
