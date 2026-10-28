package cl.belmontespa.myapplication.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF2F6F5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9EBE6),
    onPrimaryContainer = Color(0xFF2F6F5E),

    secondary = Color(0xFF2F6F5E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9EBE6),
    onSecondaryContainer = Color(0xFF2F6F5E),

    tertiary = Color(0xFF37689C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE8F2),
    onTertiaryContainer = Color(0xFF37689C),

    background = Color(0xFFF3F2EE),
    onBackground = Color(0xFF211F1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF211F1C),
    surfaceVariant = Color(0xFFECEAE3),
    onSurfaceVariant = Color(0xFF696864),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFECEAE3),

    outline = Color(0xFF4A5560),
    outlineVariant = Color(0xFFD8D6D0),

    error = Color(0xFFA3402F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF5E1DC),
    onErrorContainer = Color(0xFFA3402F),
)

val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF4FAE95),
    onPrimary = Color(0xFF08201B),
    primaryContainer = Color(0xFF1F3630),
    onPrimaryContainer = Color(0xFF4FAE95),

    secondary = Color(0xFF4FAE95),
    onSecondary = Color(0xFF08201B),
    secondaryContainer = Color(0xFF1F3630),
    onSecondaryContainer = Color(0xFF4FAE95),

    tertiary = Color(0xFF7FB0E0),
    onTertiary = Color(0xFF15171A),
    tertiaryContainer = Color(0xFF1F2C37),
    onTertiaryContainer = Color(0xFF7FB0E0),

    background = Color(0xFF15171A),
    onBackground = Color(0xFFEEF0EE),
    surface = Color(0xFF1D2023),
    onSurface = Color(0xFFEEF0EE),
    surfaceVariant = Color(0xFF262A2D),
    onSurfaceVariant = Color(0xFF9EA3A1),

    surfaceContainerLowest = Color(0xFF1D2023),
    surfaceContainerLow = Color(0xFF1D2023),
    surfaceContainer = Color(0xFF1D2023),
    surfaceContainerHigh = Color(0xFF1D2023),
    surfaceContainerHighest = Color(0xFF262A2D),

    outline = Color(0xFF7C8790),
    outlineVariant = Color(0xFF2C2F33),

    error = Color(0xFFE08A78),
    onError = Color(0xFF2A0F0A),
    errorContainer = Color(0xFF3A221D),
    onErrorContainer = Color(0xFFE08A78),
)

data class ColoresExtendidos(
    val aviso: Color,
    val contenedorAviso: Color,
)

val ExtendidosClaro = ColoresExtendidos(
    aviso = Color(0xFF8A6514),
    contenedorAviso = Color(0xFFF6ECD2),
)

val ExtendidosOscuro = ColoresExtendidos(
    aviso = Color(0xFFD9B45A),
    contenedorAviso = Color(0xFF352C17),
)
