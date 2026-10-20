package cl.belmontespa.myapplication.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.ui.auth.LoginScreen
import cl.belmontespa.myapplication.ui.components.PantallaCargando

/**
 * Raíz de la app: un `NavHost` con el flujo de acceso y uno por rol. El rol lo decide la sesión, no
 * un selector; esto no reemplaza la autorización del backend.
 */
@Composable
fun BelmonteApp(
    estadoSesion: EstadoSesion?,
    onCerrarSesion: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val entrada by navController.currentBackStackEntryAsState()
    val destinoActual = entrada?.destination
    val items = (estadoSesion as? EstadoSesion.Autenticada)?.sesion?.rol?.let(::itemsBarra).orEmpty()
    val mostrarBarra = items.any { destinoActual?.esRuta(it.ruta) == true }

    // Iniciar sesión borra el flujo de acceso del historial y cerrarla borra el flujo autenticado.
    LaunchedEffect(estadoSesion) {
        val destino = when (estadoSesion) {
            null -> return@LaunchedEffect
            EstadoSesion.SinSesion -> GrafoAcceso
            is EstadoSesion.Temporal -> CambioPasswordObligatorio
            is EstadoSesion.Autenticada -> grafoDe(estadoSesion.sesion.rol)
        }
        if (debeNavegar(navController.currentDestination, destino)) {
            navController.navigate(destino) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        // Cada pantalla maneja la barra de estado; aquí solo los bordes laterales e inferior.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            if (mostrarBarra) {
                BarraInferiorRol(items, destinoActual) { item ->
                    navController.navigate(item.ruta) {
                        popUpTo(items.first().ruta) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Inicio, modifier = Modifier.padding(padding)) {
            composable<Inicio> {
                PantallaCargando(
                    texto = stringResource(R.string.inicio_comprobando_sesion),
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            navigation<GrafoAcceso>(startDestination = Login) {
                composable<Login> {
                    LoginScreen(
                        onCrearCuenta = { navController.navigate(RegistroSolicitante) },
                        onRecuperarPassword = { navController.navigate(RecuperarPassword) },
                        onAbordarViaje = { navController.navigate(AbordarViaje) },
                    )
                }
                composable<RegistroSolicitante> {
                    PantallaProvisoria(R.string.titulo_registro_solicitante, onVolver = navController::popBackStack)
                }
                composable<RecuperarPassword> {
                    PantallaProvisoria(R.string.titulo_recuperar_password, onVolver = navController::popBackStack)
                }
                composable<AbordarViaje> {
                    PantallaProvisoria(R.string.titulo_abordar_viaje, onVolver = navController::popBackStack)
                }
                composable<CambioPasswordObligatorio> {
                    CambioPasswordObligatorioProvisorio(onCerrarSesion)
                }
            }

            navigation<GrafoSolicitante>(startDestination = MisSolicitudes) {
                composable<MisSolicitudes> { PantallaProvisoria(R.string.nav_mis_solicitudes) }
                composable<PerfilSolicitante> { PerfilProvisorio(onCerrarSesion) }
            }

            navigation<GrafoAdministrador>(startDestination = Solicitudes) {
                composable<Solicitudes> { PantallaProvisoria(R.string.nav_solicitudes) }
                composable<Conductores> { PantallaProvisoria(R.string.nav_conductores) }
                composable<Vehiculos> { PantallaProvisoria(R.string.nav_vehiculos) }
                composable<PerfilAdministrador> { PerfilProvisorio(onCerrarSesion) }
            }

            navigation<GrafoConductor>(startDestination = MisViajes) {
                composable<MisViajes> { PantallaProvisoria(R.string.nav_mis_viajes) }
                composable<PerfilConductor> { PerfilProvisorio(onCerrarSesion) }
            }
        }
    }
}

/**
 * Navega solo si la pantalla actual no pertenece ya al flujo pedido: así una rotación no reinicia el
 * historial. "Cambio obligatorio de contraseña" se deja siempre que la sesión deja de ser temporal.
 */
private fun debeNavegar(destinoActual: NavDestination?, destino: Any): Boolean {
    if (destinoActual == null) return true
    if (destinoActual.hasRoute(CambioPasswordObligatorio::class) && destino != CambioPasswordObligatorio) return true
    return !destinoActual.esRuta(destino)
}
