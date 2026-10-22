package cl.belmontespa.myapplication.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.theme.EstilosMono

/**
 * Barra superior plana con divisor: título, subtítulo monoespaciado opcional (p. ej. el folio) y
 * flecha "Volver" en formularios y detalles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier) {
        // El título puede ocupar más de una línea con el texto ampliado.
        TopAppBar(
            expandedHeight = 64.dp * LocalDensity.current.fontScale.coerceAtLeast(1f),
            title = {
                Column {
                    Text(titulo, modifier = Modifier.semantics { heading() })
                    if (subtitulo != null) {
                        Text(
                            subtitulo,
                            style = EstilosMono.subtitulo,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            navigationIcon = {
                // La pantalla decide si permite volver y qué acción corresponde.
                if (onVolver != null) {
                    IconButton(
                        onClick = onVolver,
                        // Garantiza un área táctil mínima de 48 × 48 dp.
                        modifier = Modifier.sizeIn(
                            minWidth = 48.dp,
                            minHeight = 48.dp,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_atras),
                            // TalkBack anuncia la acción del botón.
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                }
            },
            actions = acciones,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
