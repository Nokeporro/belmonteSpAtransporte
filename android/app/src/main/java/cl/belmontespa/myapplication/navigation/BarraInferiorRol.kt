package cl.belmontespa.myapplication.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.view.components.etiqueta

/** Destino principal de un rol, con su ícono en la barra inferior. `ruta` es la de su pantalla. */
data class ItemBarra(
    val ruta: String,
    @param:StringRes val etiqueta: Int,
    @param:DrawableRes val icono: Int,
)

/** Barra inferior de cada rol; el primer ítem es su pantalla de inicio. */
fun itemsBarra(rol: Rol): List<ItemBarra> = when (rol) {
    Rol.SOLICITANTE -> listOf(
        ItemBarra(PantallaDemo.MIS_SOLICITUDES.ruta, R.string.nav_mis_solicitudes, R.drawable.ic_lista),
        ItemBarra(PantallaDemo.PERFIL_SOLICITANTE.ruta, R.string.nav_perfil, R.drawable.ic_usuario),
    )
    Rol.ADMINISTRADOR -> listOf(
        ItemBarra(PantallaDemo.SOLICITUDES.ruta, R.string.nav_solicitudes, R.drawable.ic_lista),
        ItemBarra(PantallaDemo.CONDUCTORES.ruta, R.string.nav_conductores, R.drawable.ic_usuarios),
        ItemBarra(PantallaDemo.VEHICULOS.ruta, R.string.nav_vehiculos, R.drawable.ic_auto),
        ItemBarra(PantallaDemo.PERFIL_ADMIN.ruta, R.string.nav_perfil, R.drawable.ic_usuario),
    )
    Rol.CONDUCTOR -> listOf(
        ItemBarra(PantallaDemo.MIS_VIAJES.ruta, R.string.nav_mis_viajes, R.drawable.ic_calendario),
        ItemBarra(PantallaDemo.PERFIL_CONDUCTOR.ruta, R.string.nav_perfil, R.drawable.ic_usuario),
    )
}

/** Verdadero si el destino es esa ruta o está dentro de ese grafo. */
fun NavDestination.esRuta(ruta: String): Boolean = hierarchy.any { it.route == ruta }

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
