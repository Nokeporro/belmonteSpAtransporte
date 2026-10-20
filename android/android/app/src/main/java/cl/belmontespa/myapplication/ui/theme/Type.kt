package cl.belmontespa.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.belmontespa.myapplication.R

/** IBM Plex Mono (OFL, incluida en el APK) para folio, patente, horas y etiquetas de campo. */
val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_mono_bold, FontWeight.Bold),
)

/** Material 3 usa Roboto, la fuente del sistema, en todos sus estilos. */
val Tipografia = Typography()

/** Estilos monoespaciados; los tamaños van en sp para respetar la fuente del sistema. */
object EstilosMono {
    /** Folio, patente, horas y montos dentro de un texto. */
    val dato = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 14.sp)

    /** Subtítulo de la barra superior (p. ej. el folio). */
    val subtitulo = TextStyle(fontFamily = IbmPlexMono, fontSize = 13.sp, letterSpacing = 0.2.sp)

    /** Etiqueta de campo o de sección, en mayúsculas. */
    val etiqueta = TextStyle(fontFamily = IbmPlexMono, fontSize = 12.sp, letterSpacing = 0.6.sp)

    /** Texto de un chip de estado normal y grande. */
    val chip = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp)
    val chipGrande = chip.copy(fontSize = 13.sp)

    /** Marca "BELMONTE" del Login. */
    val marca = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 1.5.sp)
}
