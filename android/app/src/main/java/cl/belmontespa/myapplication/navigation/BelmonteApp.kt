package cl.belmontespa.myapplication.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.view.auth.LoginScreen
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.BotonAncho
import cl.belmontespa.myapplication.view.components.PantallaCargando
import cl.belmontespa.myapplication.view.components.TonoAviso

/** Un flujo por rol y rutas de T06 que solo transportan identificadores. */
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
    var previa by remember { mutableStateOf<EstadoSesion?>(null) }
    var cierreManual by remember { mutableStateOf(false) }
    var correoRegistro by remember { mutableStateOf("") }
    val cerrar: () -> Unit = { cierreManual = true; onCerrarSesion() }
    val volver: () -> Unit = { navController.popBackStack() }

    val navegar: (PantallaDemo, String) -> Unit = { pantalla, id ->
        val destino: Any = when (pantalla) {
            PantallaDemo.LOGIN, PantallaDemo.LOGIN_DEMO -> {
                correoRegistro = id
                (estadoSesion as? EstadoSesion.Autenticada)?.sesion?.rol?.let(::grafoDe) ?: GrafoAcceso
            }
            PantallaDemo.MIS_SOLICITUDES -> MisSolicitudes
            PantallaDemo.SOLICITUDES -> Solicitudes
            PantallaDemo.MIS_VIAJES -> MisViajes
            else -> DestinoDemo(pantalla, id)
        }
        val rutaActual = navController.currentBackStackEntry?.takeIf { it.destination.hasRoute(DestinoDemo::class) }?.toRoute<DestinoDemo>()
        val anterior = navController.previousBackStackEntry?.takeIf { it.destination.hasRoute(DestinoDemo::class) }?.toRoute<DestinoDemo>()
        if (reemplazaDetalleAnterior(rutaActual, anterior, destino)) {
            // Todas las DestinoDemo comparten id: se saca el formulario y luego el detalle de abajo, que queda reemplazado.
            navController.popBackStack()
            navController.navigate(destino) { navController.currentDestination?.id?.let { popUpTo(it) { inclusive = true } } }
        } else navController.navigate(destino) {
            launchSingleTop = destino !is DestinoDemo || rutaActual == destino
            // Un envío confirmado elimina el formulario del historial.
            if (rutaActual?.pantalla in formulariosConSalida && pantalla in detallesTrasGuardar) {
                navController.currentDestination?.id?.let { popUpTo(it) { inclusive = true } }
            }
            if (pantalla in listOf(PantallaDemo.LOGIN, PantallaDemo.LOGIN_DEMO)) popUpTo(navController.graph.id) { inclusive = true }
            if (pantalla == PantallaDemo.MIS_SOLICITUDES) popUpTo(MisSolicitudes) { inclusive = true }
            if (pantalla == PantallaDemo.MIS_VIAJES) popUpTo(MisViajes) { inclusive = true }
        }
    }
    LaunchedEffect(estadoSesion) {
        val anterior = previa
        previa = estadoSesion
        val destino: Any = when (estadoSesion) {
            null -> return@LaunchedEffect
            EstadoSesion.SinSesion -> if ((anterior is EstadoSesion.Autenticada || anterior is EstadoSesion.Temporal) && !cierreManual) DestinoDemo(PantallaDemo.SESION_EXPIRADA) else GrafoAcceso
            is EstadoSesion.Temporal -> DestinoDemo(PantallaDemo.CAMBIO_OBLIGATORIO)
            is EstadoSesion.Autenticada -> grafoDe(estadoSesion.sesion.rol)
        }
        cierreManual = false
        val actual = navController.currentBackStackEntry?.takeIf { it.destination.hasRoute(DestinoDemo::class) }?.toRoute<DestinoDemo>()
        // Una rotación conserva el detalle o formulario autorizado que ya estaba abierto.
        val cambioDeCuenta = anterior is EstadoSesion.Autenticada && estadoSesion is EstadoSesion.Autenticada && anterior.sesion.correo != estadoSesion.sesion.correo
        val entrando = estadoSesion is EstadoSesion.Autenticada && (anterior == EstadoSesion.SinSesion || anterior is EstadoSesion.Temporal)
        val conservar = !entrando && !cambioDeCuenta && actual != null && permitePantalla(actual.pantalla, estadoSesion) &&
            !(estadoSesion == EstadoSesion.SinSesion && (anterior is EstadoSesion.Autenticada || anterior is EstadoSesion.Temporal)) &&
            !(estadoSesion is EstadoSesion.Autenticada && actual.pantalla == PantallaDemo.CAMBIO_OBLIGATORIO)
        val necesitaNavegar = if (destino is DestinoDemo) actual != destino else debeNavegar(navController.currentDestination, destino)
        if (!conservar && necesitaNavegar) {
            // No se restaura el historial de otra cuenta tras cerrar o perder la sesión.
            navController.navigate(destino) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            if (mostrarBarra) BarraInferiorRol(items, destinoActual) { item ->
                navController.navigate(item.ruta) {
                    popUpTo(items.first().ruta)
                    launchSingleTop = true

                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Inicio, modifier = Modifier.padding(padding)) {
            composable<Inicio> { PantallaCargando(texto = stringResource(R.string.inicio_comprobando_sesion), modifier = Modifier.statusBarsPadding()) }
            navigation<GrafoAcceso>(startDestination = Login) {
                composable<Login> { LoginScreen(
                    onCrearCuenta = { navegar(PantallaDemo.REGISTRO, "") },
                    onRecuperarPassword = { navegar(PantallaDemo.RECUPERAR, "") },
                    onAbordarViaje = { navegar(PantallaDemo.ABORDAR, "") }, correoInicial = correoRegistro,
                ) }
            }
            navigation<GrafoSolicitante>(startDestination = MisSolicitudes) {
                composable<MisSolicitudes> { DestinoPantalla(PantallaDemo.MIS_SOLICITUDES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<PerfilSolicitante> { DestinoPantalla(PantallaDemo.PERFIL_SOLICITANTE, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
            }
            navigation<GrafoAdministrador>(startDestination = Solicitudes) {
                composable<Solicitudes> { DestinoPantalla(PantallaDemo.SOLICITUDES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<Conductores> { DestinoPantalla(PantallaDemo.CONDUCTORES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<Vehiculos> { DestinoPantalla(PantallaDemo.VEHICULOS, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<PerfilAdministrador> { DestinoPantalla(PantallaDemo.PERFIL_ADMIN, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
            }
            navigation<GrafoConductor>(startDestination = MisViajes) {
                composable<MisViajes> { DestinoPantalla(PantallaDemo.MIS_VIAJES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<PerfilConductor> { DestinoPantalla(PantallaDemo.PERFIL_CONDUCTOR, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
            }
            composable<DestinoDemo> { entradaDemo ->
                val ruta = entradaDemo.toRoute<DestinoDemo>()
                if (permitePantalla(ruta.pantalla, estadoSesion)) {
                    DestinoPantalla(ruta.pantalla, ruta.id, navegar, volver, cerrar)
                } else Column(Modifier.padding(24.dp)) {
                    Aviso(stringResource(R.string.error_sin_permiso), TonoAviso.PELIGRO)
                    BotonAncho(stringResource(R.string.accion_volver), volver)
                }
            }
        }
    }
}

// Protección local de rutas. Las operaciones vuelven a validar el alcance en el repositorio.
internal fun permitePantalla(pantalla: PantallaDemo, estado: EstadoSesion?): Boolean = when (estado) {
    is EstadoSesion.Autenticada -> (pantalla.rol == null || pantalla.rol == estado.sesion.rol) &&
        !(estado.sesion.rol == Rol.CONDUCTOR && pantalla in listOf(PantallaDemo.CAMBIAR_PASSWORD, PantallaDemo.DETALLE_PAGO))
    is EstadoSesion.Temporal -> pantalla == PantallaDemo.CAMBIO_OBLIGATORIO
    EstadoSesion.SinSesion -> pantalla.publica
    null -> false
}
/**
 * Un formulario guardado que vuelve al detalle del viaje del que salió (Asignar, Cancelar viaje, Agregar pasajero) lo
 * reemplaza en vez de apilar otro igual: así «atrás» vuelve a la lista y no al mismo viaje.
 */
internal fun reemplazaDetalleAnterior(formulario: DestinoDemo?, anterior: DestinoDemo?, destino: Any): Boolean =
    formulario?.pantalla in formulariosConSalida && destino is DestinoDemo && destino.pantalla in detallesDeViaje &&
        anterior != null && anterior.pantalla in detallesDeViaje && anterior.id == destino.id
private fun debeNavegar(actual: NavDestination?, destino: Any): Boolean = actual == null || !actual.esRuta(destino)
private val formulariosConSalida = setOf(PantallaDemo.NUEVA_SOLICITUD, PantallaDemo.AGREGAR_PASAJERO, PantallaDemo.CONFIRMAR_CANCELACION, PantallaDemo.CANCELAR_VIAJE, PantallaDemo.ASIGNAR, PantallaDemo.REGISTRAR_CONDUCTOR, PantallaDemo.REGISTRAR_VEHICULO, PantallaDemo.REGISTRAR_PAGO, PantallaDemo.CONFIRMAR_ABORDAJE)
private val detallesDeViaje = setOf(PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.SOLICITUD_SIN_PASAJEROS, PantallaDemo.DETALLE_ADMIN, PantallaDemo.SIN_PASAJEROS_ADMIN)
private val detallesTrasGuardar = setOf(PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.DETALLE_ADMIN, PantallaDemo.DETALLE_VIAJE, PantallaDemo.DETALLE_CONDUCTOR, PantallaDemo.DETALLE_VEHICULO, PantallaDemo.DETALLE_PAGO, PantallaDemo.SOLICITUD_CANCELADA, PantallaDemo.ABORDADO)
