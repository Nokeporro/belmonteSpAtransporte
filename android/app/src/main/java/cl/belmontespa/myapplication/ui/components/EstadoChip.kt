package cl.belmontespa.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.theme.BelmonteTema
import cl.belmontespa.myapplication.ui.theme.EstilosMono

enum class EstiloChip {
    NEUTRO,
    INFORMACION,
    LLENO,
    TENUE,
    CORRECTO,
    PELIGRO,
    PELIGRO_BORDE,
    AVISO,
}

private data class ColoresChip(
    val texto: Color,
    val borde: Color,
    val fondo: Color,
)

@Composable
private fun EstiloChip.colores(): ColoresChip {
    val esquema = MaterialTheme.colorScheme
    val extendidos = BelmonteTema.extendidos

    return when (this) {
        EstiloChip.NEUTRO -> ColoresChip(
            esquema.onSurfaceVariant,
            esquema.outline,
            Color.Transparent,
        )

        EstiloChip.INFORMACION -> ColoresChip(
            esquema.tertiary,
            esquema.tertiary,
            esquema.tertiaryContainer,
        )

        EstiloChip.LLENO -> ColoresChip(
            esquema.onPrimary,
            esquema.primary,
            esquema.primary,
        )

        EstiloChip.TENUE -> ColoresChip(
            esquema.onSurfaceVariant,
            esquema.outlineVariant,
            esquema.surfaceVariant,
        )

        EstiloChip.CORRECTO -> ColoresChip(
            esquema.primary,
            esquema.primary,
            esquema.primaryContainer,
        )

        EstiloChip.PELIGRO -> ColoresChip(
            esquema.error,
            esquema.error,
            esquema.errorContainer,
        )

        EstiloChip.PELIGRO_BORDE -> ColoresChip(
            esquema.error,
            esquema.error,
            Color.Transparent,
        )

        EstiloChip.AVISO -> ColoresChip(
            extendidos.aviso,
            extendidos.aviso,
            extendidos.contenedorAviso,
        )
    }
}

@Composable
fun EstadoChip(
    etiqueta: String,
    estilo: EstiloChip,
    modifier: Modifier = Modifier,
    grande: Boolean = false,
) {
    val colores = estilo.colores()
    val descripcion = stringResource(
        R.string.chip_descripcion,
        etiqueta,
    )

    val espaciado = if (grande) {
        Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
    } else {
        Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
    }

    Surface(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = descripcion
        },
        shape = CircleShape,
        color = colores.fondo,
        contentColor = colores.texto,
        border = BorderStroke(1.dp, colores.borde),
    ) {
        Text(
            text = etiqueta.uppercase(),
            style = if (grande) {
                EstilosMono.chipGrande
            } else {
                EstilosMono.chip
            },
            modifier = espaciado,
        )
    }
}
