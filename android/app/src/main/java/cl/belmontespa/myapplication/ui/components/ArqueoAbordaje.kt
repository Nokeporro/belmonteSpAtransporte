package cl.belmontespa.myapplication.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Arqueo
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.util.Formatos

/**
 * Arqueo de abordaje (README, RF29): «Abordaron X de N. Faltan: …» y un chip por pasajero. Es el mismo en el
 * detalle del Conductor y en el del Administrador; `Viaje.arqueo()` decide en qué estados existe.
 */
@Composable
fun ArqueoDeAbordaje(arqueo: Arqueo, pasajeros: List<PasajeroViaje>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
