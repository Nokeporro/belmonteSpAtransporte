package cl.belmontespa.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColoresExtendidos = staticCompositionLocalOf { ExtendidosClaro }

/** Material 3 sin color dinámico (la marca no cambia con el fondo de pantalla); claro u oscuro según el sistema. */
@Composable
fun BelmonteTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalColoresExtendidos provides if (oscuro) ExtendidosOscuro else ExtendidosClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = Tipografia,
            content = contenido,
        )
    }
}

/** Acceso a lo que el tema agrega a Material 3. */
object BelmonteTema {
    val extendidos: ColoresExtendidos
        @Composable get() = LocalColoresExtendidos.current
}
