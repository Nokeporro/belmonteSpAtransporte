package cl.belmontespa.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.theme.BelmonteTema

enum class TonoAviso { INFORMACION, AVISO, PELIGRO }

/** Recuadro de aviso: texto y borde del tono sobre su fondo suave (p. ej. "Faltan pasajeros…"). */
@Composable
fun Aviso(
    texto: String,
    tono: TonoAviso,
    modifier: Modifier = Modifier,
) {
    val (color, fondo) = coloresTono(tono)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = fondo,
        contentColor = color,
        border = BorderStroke(1.dp, color),
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/** Banner del Conductor sin conexión: "Sin conexión. Los cambios se enviarán al reconectar." */
@Composable
fun BannerSinConexion(modifier: Modifier = Modifier) {
    val (color, fondo) = coloresTono(TonoAviso.AVISO)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = fondo,
        contentColor = color,
        border = BorderStroke(1.dp, color),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_sin_red), contentDescription = null)
            Text(stringResource(R.string.banner_sin_conexion_conductor), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun coloresTono(tono: TonoAviso): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (tono) {
        TonoAviso.INFORMACION -> c.tertiary to c.tertiaryContainer
        TonoAviso.AVISO -> BelmonteTema.extendidos.aviso to BelmonteTema.extendidos.contenedorAviso
        TonoAviso.PELIGRO -> c.error to c.errorContainer
    }
}
