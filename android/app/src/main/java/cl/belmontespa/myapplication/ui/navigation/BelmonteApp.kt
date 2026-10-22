package cl.belmontespa.myapplication.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.ui.auth.LoginScreen
import cl.belmontespa.myapplication.ui.components.*
import cl.belmontespa.myapplication.ui.pantallas.PantallaT06

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
        navController.navigate(destino) {
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
                composable<RegistroSolicitante> { PantallaT06(PantallaDemo.REGISTRO, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar) }
                composable<RecuperarPassword> { PantallaT06(PantallaDemo.RECUPERAR, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar) }
                composable<AbordarViaje> { PantallaT06(PantallaDemo.ABORDAR, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar) }
                composable<CambioPasswordObligatorio> { PantallaT06(PantallaDemo.CAMBIO_OBLIGATORIO, onNavegar = navegar, onVolver = cerrar, onCerrarSesion = cerrar) }
            }
            navigation<GrafoSolicitante>(startDestination = MisSolicitudes) {
                composable<MisSolicitudes> { PantallaT06(PantallaDemo.MIS_SOLICITUDES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<PerfilSolicitante> { PantallaT06(PantallaDemo.PERFIL_SOLICITANTE, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<NuevaSolicitud> { PantallaT06(PantallaDemo.NUEVA_SOLICITUD, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar) }
                composable<DetalleSolicitud> { PantallaT06(PantallaDemo.DETALLE_SOLICITUD, it.toRoute<DetalleSolicitud>().folio, navegar, volver, cerrar) }
                composable<AgregarPasajero> { PantallaT06(PantallaDemo.AGREGAR_PASAJERO, it.toRoute<AgregarPasajero>().folio, navegar, volver, cerrar) }
                composable<ConfirmarCancelacionSolicitud> { PantallaT06(PantallaDemo.CONFIRMAR_CANCELACION, it.toRoute<ConfirmarCancelacionSolicitud>().folio, navegar, volver, cerrar) }
                composable<SolicitudCancelada> { PantallaT06(PantallaDemo.SOLICITUD_CANCELADA, it.toRoute<SolicitudCancelada>().folio, navegar, volver, cerrar) }
                composable<CambiarPassword> { PantallaT06(PantallaDemo.CAMBIAR_PASSWORD, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar) }
            }
            navigation<GrafoAdministrador>(startDestination = Solicitudes) {
                composable<Solicitudes> { PantallaT06(PantallaDemo.SOLICITUDES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<Conductores> { PantallaT06(PantallaDemo.CONDUCTORES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<Vehiculos> { PantallaT06(PantallaDemo.VEHICULOS, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<PerfilAdministrador> { PantallaT06(PantallaDemo.PERFIL_ADMIN, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
            }
            navigation<GrafoConductor>(startDestination = MisViajes) {
                composable<MisViajes> { PantallaT06(PantallaDemo.MIS_VIAJES, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
                composable<PerfilConductor> { PantallaT06(PantallaDemo.PERFIL_CONDUCTOR, onNavegar = navegar, onVolver = volver, onCerrarSesion = cerrar, raiz = true) }
            }
            composable<DestinoDemo> { entradaDemo ->
                val ruta = entradaDemo.toRoute<DestinoDemo>()
                if (permitePantalla(ruta.pantalla, estadoSesion)) {
                    PantallaT06(ruta.pantalla, ruta.id, navegar, volver, cerrar)
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
private fun debeNavegar(actual: NavDestination?, destino: Any): Boolean = actual == null || !actual.esRuta(destino)
private val formulariosConSalida = setOf(PantallaDemo.NUEVA_SOLICITUD, PantallaDemo.AGREGAR_PASAJERO, PantallaDemo.CONFIRMAR_CANCELACION, PantallaDemo.CANCELAR_VIAJE, PantallaDemo.ASIGNAR, PantallaDemo.REGISTRAR_CONDUCTOR, PantallaDemo.REGISTRAR_VEHICULO, PantallaDemo.REGISTRAR_PAGO, PantallaDemo.CONFIRMAR_INICIO, PantallaDemo.CONFIRMAR_ABORDAJE)
private val detallesTrasGuardar = setOf(PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.DETALLE_ADMIN, PantallaDemo.DETALLE_VIAJE, PantallaDemo.DETALLE_CONDUCTOR, PantallaDemo.DETALLE_VEHICULO, PantallaDemo.DETALLE_PAGO, PantallaDemo.SOLICITUD_CANCELADA, PantallaDemo.ABORDADO)
