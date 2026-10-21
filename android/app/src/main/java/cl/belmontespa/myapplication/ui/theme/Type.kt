package cl.belmontespa.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.belmontespa.myapplication.R

val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_mono_bold, FontWeight.Bold),
)

val Tipografia = Typography()

object EstilosMono {

    val dato = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    )

    val subtitulo = TextStyle(
        fontFamily = IbmPlexMono,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    )

    val etiqueta = TextStyle(
        fontFamily = IbmPlexMono,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp,
    )

    val chip = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
    )

    val chipGrande = chip.copy(
        fontSize = 13.sp,
    )

    val marca = TextStyle(
        fontFamily = IbmPlexMono,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 1.5.sp,
    )
}
