package cl.belmontespa.myapplication.ui.navigation

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol

/** Destino principal de un rol, con su ícono en la barra inferior. */
data class ItemBarra(
    val ruta: Any,
    @param:StringRes val etiqueta: Int,
    @param:DrawableRes val icono: Int,
)

/** Barra inferior de cada rol; el primer ítem es su pantalla de inicio. */
fun itemsBarra(rol: Rol): List<ItemBarra> = when (rol) {
    Rol.SOLICITANTE -> listOf(
        ItemBarra(MisSolicitudes, R.string.nav_mis_solicitudes, R.drawable.ic_lista),
        ItemBarra(PerfilSolicitante, R.string.nav_perfil, R.drawable.ic_usuario),
    )
    Rol.ADMINISTRADOR -> listOf(
        ItemBarra(Solicitudes, R.string.nav_solicitudes, R.drawable.ic_lista),
        ItemBarra(Conductores, R.string.nav_conductores, R.drawable.ic_usuarios),
        ItemBarra(Vehiculos, R.string.nav_vehiculos, R.drawable.ic_auto),
        ItemBarra(PerfilAdministrador, R.string.nav_perfil, R.drawable.ic_usuario),
    )
    Rol.CONDUCTOR -> listOf(
        ItemBarra(MisViajes, R.string.nav_mis_viajes, R.drawable.ic_calendario),
        ItemBarra(PerfilConductor, R.string.nav_perfil, R.drawable.ic_usuario),
    )
}

/** Grafo de navegación del rol que entrega el login. */
fun grafoDe(rol: Rol): Any = when (rol) {
    Rol.SOLICITANTE -> GrafoSolicitante
    Rol.ADMINISTRADOR -> GrafoAdministrador
    Rol.CONDUCTOR -> GrafoConductor
}

fun NavDestination.esRuta(ruta: Any): Boolean = hierarchy.any { it.hasRoute(ruta::class) }

@Composable
fun BarraInferiorRol(
    items: List<ItemBarra>,
    destinoActual: NavDestination?,
    onElegir: (ItemBarra) -> Unit,
) {
    NavigationBar(modifier = Modifier.heightIn(min = 80.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)), containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            NavigationBarItem(
                selected = destinoActual?.esRuta(item.ruta) == true,
                onClick = { onElegir(item) },
                icon = { Icon(painterResource(item.icono), contentDescription = null) },
                label = { Text(stringResource(item.etiqueta)) },
            )
        }
    }
}
