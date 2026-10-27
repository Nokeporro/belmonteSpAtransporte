package cl.belmontespa.myapplication.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol

/**
 * La app: la barra inferior del rol y el NavHost (NavGrafos.kt). Aquí vive lo que decide a dónde ir cuando cambia la
 * sesión (login, sesión vencida, cambio de contraseña obligatorio) y cómo se arma el historial al navegar.
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
    val rol = (estadoSesion as? EstadoSesion.Autenticada)?.sesion?.rol
    var previa by remember { mutableStateOf<EstadoSesion?>(null) }
    var cierreManual by remember { mutableStateOf(false) }
    val cerrar: () -> Unit = { cierreManual = true; onCerrarSesion() }
    val volver: () -> Unit = { navController.popBackStack() }
    val navegar: (PantallaDemo, String) -> Unit = { pantalla, id -> navController.irA(pantalla, id, rol) }

    LaunchedEffect(estadoSesion) {
        val anterior = previa
        previa = estadoSesion
        val estado = estadoSesion ?: return@LaunchedEffect
        val destino = destinoPorSesion(anterior, estado, cierreManual)
        cierreManual = false
        val actual = navController.currentBackStackEntry.destino()
        // Una rotación conserva el detalle o formulario autorizado que ya estaba abierto.
        if (conservarPantalla(anterior, estado, actual)) return@LaunchedEffect
        val grafo = grafoDeSesion(destino, estado)
        val yaEstaAhi = if (grafo != null) navController.currentDestination?.esRuta(grafo) == true else actual == destino
        if (!yaEstaAhi) {
            // No se restaura el historial de otra cuenta tras cerrar o perder la sesión.
            navController.navigate(grafo ?: destino.ruta()) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
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
        val acciones = AccionesNav(estadoSesion, navegar, volver, cerrar)
        NavHost(navController, startDestination = PantallaDemo.INICIO.ruta, modifier = Modifier.padding(padding)) {
            pantallaInicio()
            grafoAcceso(acciones)
            grafoSolicitante(acciones)
            grafoAdministrador(acciones)
            grafoConductor(acciones)
        }
    }
}

// ---- Navegar entre pantallas ----

/** Pantallas raíz: no se apilan sobre otras (el inicio, el Login y las de la barra inferior). */
private val pantallasRaiz = setOf(
    PantallaDemo.INICIO, PantallaDemo.LOGIN,
    PantallaDemo.MIS_SOLICITUDES, PantallaDemo.PERFIL_SOLICITANTE,
    PantallaDemo.SOLICITUDES, PantallaDemo.CONDUCTORES, PantallaDemo.VEHICULOS, PantallaDemo.PERFIL_ADMIN,
    PantallaDemo.MIS_VIAJES, PantallaDemo.PERFIL_CONDUCTOR,
)
private val formulariosConSalida = setOf(
    PantallaDemo.NUEVA_SOLICITUD, PantallaDemo.AGREGAR_PASAJERO, PantallaDemo.CONFIRMAR_CANCELACION,
    PantallaDemo.CANCELAR_VIAJE, PantallaDemo.ASIGNAR, PantallaDemo.REGISTRAR_CONDUCTOR,
    PantallaDemo.REGISTRAR_VEHICULO, PantallaDemo.REGISTRAR_PAGO, PantallaDemo.CONFIRMAR_ABORDAJE,
)
private val detallesDeViaje = setOf(
    PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.SOLICITUD_SIN_PASAJEROS,
    PantallaDemo.DETALLE_ADMIN, PantallaDemo.SIN_PASAJEROS_ADMIN,
)
private val detallesTrasGuardar = setOf(
    PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.DETALLE_ADMIN, PantallaDemo.DETALLE_VIAJE,
    PantallaDemo.DETALLE_CONDUCTOR, PantallaDemo.DETALLE_VEHICULO, PantallaDemo.DETALLE_PAGO,
    PantallaDemo.SOLICITUD_CANCELADA, PantallaDemo.ABORDADO,
)

/** La pantalla en la que está una entrada del historial; `null` si es una raíz (inicio, Login o barra inferior). */
internal fun NavBackStackEntry?.destino(): Destino? {
    val pantalla = PantallaDemo.deRuta(this?.destination?.route) ?: return null
    if (pantalla in pantallasRaiz) return null
    return Destino(pantalla, this?.arguments?.getString(PantallaDemo.ARGUMENTO_ID).orEmpty())
}

/** Lleva a `pantalla` armando el historial que describe el README (Comunes: volver, guardar y salir). */
private fun NavHostController.irA(pantalla: PantallaDemo, id: String, rol: Rol?) {
    when (pantalla) {
        // «Volver al inicio»: el Login (con el correo recién registrado) o el inicio del rol, sin historial.
        PantallaDemo.LOGIN, PantallaDemo.LOGIN_DEMO -> navigate(rutaDeInicio(id, rol)) {
            launchSingleTop = true
            popUpTo(graph.id) { inclusive = true }
        }
        PantallaDemo.SOLICITUDES -> navigate(pantalla.ruta) { launchSingleTop = true }
        PantallaDemo.MIS_SOLICITUDES, PantallaDemo.MIS_VIAJES -> navigate(pantalla.ruta) {
            launchSingleTop = true
            popUpTo(pantalla.ruta) { inclusive = true }
        }
        else -> apilar(Destino(pantalla, id))
    }
}

private fun rutaDeInicio(correo: String, rol: Rol?): String =
    if (rol != null) Rutas.grafoDe(rol) else Destino(PantallaDemo.LOGIN, correo).ruta()

private fun NavHostController.apilar(destino: Destino) {
    val actual = currentBackStackEntry.destino()
    val anterior = previousBackStackEntry.destino()
    if (reemplazaDetalleAnterior(actual, anterior, destino)) {
        // Se saca el formulario y luego el detalle de abajo, que queda reemplazado por el nuevo.
        popBackStack()
        navigate(destino.ruta()) { currentDestination?.id?.let { popUpTo(it) { inclusive = true } } }
    } else {
        navigate(destino.ruta()) {
            launchSingleTop = actual == destino
            // Un envío confirmado elimina el formulario del historial.
            if (actual?.pantalla in formulariosConSalida && destino.pantalla in detallesTrasGuardar) {
                currentDestination?.id?.let { popUpTo(it) { inclusive = true } }
            }
        }
    }
}

/**
 * Un formulario guardado que vuelve al detalle del viaje del que salió (Asignar, Cancelar viaje, Agregar pasajero) lo
 * reemplaza en vez de apilar otro igual: así «atrás» vuelve a la lista y no al mismo viaje.
 */
internal fun reemplazaDetalleAnterior(formulario: Destino?, anterior: Destino?, destino: Destino): Boolean =
    formulario?.pantalla in formulariosConSalida && destino.pantalla in detallesDeViaje &&
        anterior != null && anterior.pantalla in detallesDeViaje && anterior.id == destino.id

// ---- Reaccionar a la sesión ----

/** Protección local de rutas. Las operaciones vuelven a validar el alcance en el repositorio. */
internal fun permitePantalla(pantalla: PantallaDemo, estado: EstadoSesion?): Boolean = when (estado) {
    is EstadoSesion.Autenticada -> (pantalla.rol == null || pantalla.rol == estado.sesion.rol) &&
        !(estado.sesion.rol == Rol.CONDUCTOR && pantalla in listOf(PantallaDemo.CAMBIAR_PASSWORD, PantallaDemo.DETALLE_PAGO))
    is EstadoSesion.Temporal -> pantalla == PantallaDemo.CAMBIO_OBLIGATORIO
    EstadoSesion.SinSesion -> pantalla.publica
    null -> false
}

/**
 * A dónde ir cuando cambia la sesión: el inicio del rol al entrar, el cambio obligatorio con una contraseña temporal,
 * «Sesión expirada» si la sesión se perdió sola y el Login si el usuario cerró sesión.
 */
internal fun destinoPorSesion(anterior: EstadoSesion?, estado: EstadoSesion, cierreManual: Boolean): Destino = when (estado) {
    EstadoSesion.SinSesion -> {
        val teniaSesion = anterior is EstadoSesion.Autenticada || anterior is EstadoSesion.Temporal
        Destino(if (teniaSesion && !cierreManual) PantallaDemo.SESION_EXPIRADA else PantallaDemo.LOGIN)
    }
    is EstadoSesion.Temporal -> Destino(PantallaDemo.CAMBIO_OBLIGATORIO)
    is EstadoSesion.Autenticada -> Destino(inicioDe(estado.sesion.rol))
}

/** La primera pantalla de la barra inferior de cada rol. */
internal fun inicioDe(rol: Rol): PantallaDemo = when (rol) {
    Rol.SOLICITANTE -> PantallaDemo.MIS_SOLICITUDES
    Rol.ADMINISTRADOR -> PantallaDemo.SOLICITUDES
    Rol.CONDUCTOR -> PantallaDemo.MIS_VIAJES
}

/** El grafo al que pertenece el destino de la sesión (acceso o el del rol); `null` si es una pantalla suelta. */
internal fun grafoDeSesion(destino: Destino, estado: EstadoSesion): String? = when {
    destino.pantalla == PantallaDemo.LOGIN -> Rutas.GRAFO_ACCESO
    estado is EstadoSesion.Autenticada -> Rutas.grafoDe(estado.sesion.rol)
    else -> null
}

/**
 * Verdadero si, al cambiar la sesión, se deja la pantalla que ya está abierta (p. ej. tras una rotación) en vez de
 * volver al inicio: no al entrar, ni al cambiar de cuenta, ni al perder la sesión, y solo si la sesión la permite.
 */
internal fun conservarPantalla(anterior: EstadoSesion?, estado: EstadoSesion, actual: Destino?): Boolean {
    val cambioDeCuenta = anterior is EstadoSesion.Autenticada && estado is EstadoSesion.Autenticada &&
        anterior.sesion.correo != estado.sesion.correo
    val entrando = estado is EstadoSesion.Autenticada &&
        (anterior == EstadoSesion.SinSesion || anterior is EstadoSesion.Temporal)
    val saliendo = estado == EstadoSesion.SinSesion &&
        (anterior is EstadoSesion.Autenticada || anterior is EstadoSesion.Temporal)
    return actual != null && !entrando && !cambioDeCuenta && !saliendo &&
        permitePantalla(actual.pantalla, estado) &&
        !(estado is EstadoSesion.Autenticada && actual.pantalla == PantallaDemo.CAMBIO_OBLIGATORIO)
}
