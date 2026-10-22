package cl.belmontespa.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColoresExtendidos = staticCompositionLocalOf {
    ExtendidosClaro
}

@Composable
fun BelmonteTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    val esquema = if (oscuro) EsquemaOscuro else EsquemaClaro
    val extendidos = if (oscuro) ExtendidosOscuro else ExtendidosClaro

    CompositionLocalProvider(
        LocalColoresExtendidos provides extendidos,
    ) {
        MaterialTheme(
            colorScheme = esquema,
            typography = Tipografia,
            content = contenido,
        )
    }
}

object BelmonteTema {
    val extendidos: ColoresExtendidos
        @Composable
        get() = LocalColoresExtendidos.current
}
