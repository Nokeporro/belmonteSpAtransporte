package cl.belmontespa.myapplication.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.view.abordaje.AbordadoScreen
import cl.belmontespa.myapplication.view.abordaje.AbordarScreen
import cl.belmontespa.myapplication.view.abordaje.ConfirmarAbordajeScreen
import cl.belmontespa.myapplication.view.administrador.AsignarScreen
import cl.belmontespa.myapplication.view.administrador.CancelarViajeScreen
import cl.belmontespa.myapplication.view.administrador.ConductoresScreen
import cl.belmontespa.myapplication.view.administrador.CuentasPendientesScreen
import cl.belmontespa.myapplication.view.administrador.DetalleConductorScreen
import cl.belmontespa.myapplication.view.administrador.DetallePagoScreen
import cl.belmontespa.myapplication.view.administrador.DetalleSolicitudAdminScreen
import cl.belmontespa.myapplication.view.administrador.DetalleVehiculoScreen
import cl.belmontespa.myapplication.view.administrador.HabilitacionesScreen
import cl.belmontespa.myapplication.view.administrador.RegistrarAdminScreen
import cl.belmontespa.myapplication.view.administrador.RegistrarConductorScreen
import cl.belmontespa.myapplication.view.administrador.RegistrarPagoScreen
import cl.belmontespa.myapplication.view.administrador.RegistrarVehiculoScreen
import cl.belmontespa.myapplication.view.administrador.SolicitudesScreen
import cl.belmontespa.myapplication.view.administrador.VehiculosScreen
import cl.belmontespa.myapplication.view.auth.CambiarPasswordScreen
import cl.belmontespa.myapplication.view.auth.LoginScreen
import cl.belmontespa.myapplication.view.auth.NotificacionScreen
import cl.belmontespa.myapplication.view.auth.PerfilScreen
import cl.belmontespa.myapplication.view.auth.RecuperarPasswordScreen
import cl.belmontespa.myapplication.view.auth.RegistroScreen
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.BotonAncho
import cl.belmontespa.myapplication.view.components.PantallaCargando
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.view.conductor.ConfirmarInicioScreen
import cl.belmontespa.myapplication.view.conductor.DetalleViajeScreen
import cl.belmontespa.myapplication.view.conductor.EscenarioDemo
import cl.belmontespa.myapplication.view.conductor.MisViajesScreen
import cl.belmontespa.myapplication.view.solicitante.AgregarPasajeroScreen
import cl.belmontespa.myapplication.view.solicitante.ConfirmarCancelacionScreen
import cl.belmontespa.myapplication.view.solicitante.DetalleSolicitudScreen
import cl.belmontespa.myapplication.view.solicitante.MisSolicitudesScreen
import cl.belmontespa.myapplication.view.solicitante.NuevaSolicitudScreen
import cl.belmontespa.myapplication.view.solicitante.SolicitudCanceladaScreen


class AccionesNav(
    val estado: EstadoSesion?,
    val navegar: (PantallaDemo, String) -> Unit,
    val volver: () -> Unit,
    val cerrarSesion: () -> Unit,
)

private val argumentoId = listOf(
    navArgument(PantallaDemo.ARGUMENTO_ID) {
        type = NavType.StringType
        defaultValue = ""
    },
)

private fun NavGraphBuilder.pantalla(
    pantalla: PantallaDemo,
    acciones: AccionesNav,
    contenido: @Composable (id: String) -> Unit,
) {
    composable(pantalla.ruta, arguments = if (pantalla.conId) argumentoId else emptyList()) { entrada ->
        if (permitePantalla(pantalla, acciones.estado)) {
            contenido(entrada.arguments?.getString(PantallaDemo.ARGUMENTO_ID).orEmpty())
        } else {
            SinPermiso(acciones.volver)
        }
    }
}

@Composable
private fun SinPermiso(onVolver: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Aviso(stringResource(R.string.error_sin_permiso), TonoAviso.PELIGRO)
        BotonAncho(stringResource(R.string.accion_volver), onVolver)
    }
}

fun NavGraphBuilder.pantallaInicio() {
    composable(PantallaDemo.INICIO.ruta) {
        PantallaCargando(texto = stringResource(R.string.inicio_comprobando_sesion), modifier = Modifier.statusBarsPadding())
    }
}

fun NavGraphBuilder.grafoAcceso(a: AccionesNav) {
    navigation(startDestination = PantallaDemo.LOGIN.ruta, route = Rutas.GRAFO_ACCESO) {
        composable(PantallaDemo.LOGIN.ruta, arguments = argumentoId) { entrada ->
            LoginScreen(
                onCrearCuenta = { a.navegar(PantallaDemo.REGISTRO, "") },
                onAbordarViaje = { a.navegar(PantallaDemo.ABORDAR, "") },
                correoInicial = entrada.arguments?.getString(PantallaDemo.ARGUMENTO_ID).orEmpty(),
            )
        }
    }
    pantalla(PantallaDemo.LOGIN_DEMO, a) {
        Box(Modifier.fillMaxSize().testTag("pantalla_contenido")) {
            LoginScreen(
                onCrearCuenta = { a.navegar(PantallaDemo.REGISTRO, "") },
                onAbordarViaje = { a.navegar(PantallaDemo.ABORDAR, "") },
                modoDemoForzado = true,
            )
        }
    }
    pantalla(PantallaDemo.SESION_EXPIRADA, a) {
        Box(Modifier.fillMaxSize().testTag("pantalla_contenido")) {
            LoginScreen(
                onCrearCuenta = { a.navegar(PantallaDemo.REGISTRO, "") },
                onAbordarViaje = { a.navegar(PantallaDemo.ABORDAR, "") },
                mostrarSesionExpirada = true,
            )
        }
    }
    pantalla(PantallaDemo.REGISTRO, a) { RegistroScreen(onNavegar = a.navegar, onVolver = a.volver) }
    pantalla(PantallaDemo.CAMBIO_OBLIGATORIO, a) {
        CambiarPasswordScreen(pantalla = PantallaDemo.CAMBIO_OBLIGATORIO, onVolver = a.volver, onCerrarSesion = a.cerrarSesion)
    }
    pantalla(PantallaDemo.ABORDAR, a) {
        AbordarScreen(pantalla = PantallaDemo.ABORDAR, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.ESCANEAR, a) {
        AbordarScreen(pantalla = PantallaDemo.ESCANEAR, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.CONFIRMAR_ABORDAJE, a) { id ->
        ConfirmarAbordajeScreen(argumento = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.ABORDADO, a) { id ->
        AbordadoScreen(hora = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.CAMBIAR_PASSWORD, a) {
        CambiarPasswordScreen(pantalla = PantallaDemo.CAMBIAR_PASSWORD, onVolver = a.volver, onCerrarSesion = a.cerrarSesion)
    }
    pantalla(PantallaDemo.NOTIFICACION, a) { NotificacionScreen(onNavegar = a.navegar, onVolver = a.volver) }
    pantalla(PantallaDemo.DETALLE_PAGO, a) { id -> DetallePagoScreen(hash = id, onVolver = a.volver) }
}

fun NavGraphBuilder.grafoSolicitante(a: AccionesNav) {
    navigation(startDestination = PantallaDemo.MIS_SOLICITUDES.ruta, route = Rutas.GRAFO_SOLICITANTE) {
        composable(PantallaDemo.MIS_SOLICITUDES.ruta) { MisSolicitudesScreen(onNavegar = a.navegar) }
        composable(PantallaDemo.PERFIL_SOLICITANTE.ruta) {
            PerfilScreen(
                pantalla = PantallaDemo.PERFIL_SOLICITANTE, onNavegar = a.navegar, onVolver = null,
                onCerrarSesion = a.cerrarSesion,
            )
        }
    }
    pantalla(PantallaDemo.NUEVA_SOLICITUD, a) { NuevaSolicitudScreen(onNavegar = a.navegar, onVolver = a.volver) }
    pantalla(PantallaDemo.DETALLE_SOLICITUD, a) { id ->
        DetalleSolicitudScreen(pantalla = PantallaDemo.DETALLE_SOLICITUD, folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.SOLICITUD_SIN_PASAJEROS, a) { id ->
        DetalleSolicitudScreen(
            pantalla = PantallaDemo.SOLICITUD_SIN_PASAJEROS, folio = id, onNavegar = a.navegar, onVolver = a.volver,
        )
    }
    pantalla(PantallaDemo.AGREGAR_PASAJERO, a) { id ->
        AgregarPasajeroScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.CONFIRMAR_CANCELACION, a) { id ->
        ConfirmarCancelacionScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.SOLICITUD_CANCELADA, a) { id ->
        SolicitudCanceladaScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
}

fun NavGraphBuilder.grafoAdministrador(a: AccionesNav) {
    navigation(startDestination = PantallaDemo.SOLICITUDES.ruta, route = Rutas.GRAFO_ADMINISTRADOR) {
        composable(PantallaDemo.SOLICITUDES.ruta) { SolicitudesScreen(onNavegar = a.navegar) }
        composable(PantallaDemo.CONDUCTORES.ruta) { ConductoresScreen(onNavegar = a.navegar) }
        composable(PantallaDemo.VEHICULOS.ruta) { VehiculosScreen(onNavegar = a.navegar) }
        composable(PantallaDemo.PERFIL_ADMIN.ruta) {
            PerfilScreen(
                pantalla = PantallaDemo.PERFIL_ADMIN, onNavegar = a.navegar, onVolver = null,
                onCerrarSesion = a.cerrarSesion,
            )
        }
    }
    pantalla(PantallaDemo.CUENTAS_PENDIENTES, a) { CuentasPendientesScreen(onVolver = a.volver) }
    pantalla(PantallaDemo.DETALLE_ADMIN, a) { id ->
        DetalleSolicitudAdminScreen(pantalla = PantallaDemo.DETALLE_ADMIN, folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.SIN_PASAJEROS_ADMIN, a) { id ->
        DetalleSolicitudAdminScreen(
            pantalla = PantallaDemo.SIN_PASAJEROS_ADMIN, folio = id, onNavegar = a.navegar, onVolver = a.volver,
        )
    }
    pantalla(PantallaDemo.CANCELAR_VIAJE, a) { id ->
        CancelarViajeScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.ASIGNAR, a) { id -> AsignarScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver) }
    pantalla(PantallaDemo.REGISTRAR_CONDUCTOR, a) {
        RegistrarConductorScreen(onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.DETALLE_CONDUCTOR, a) { id ->
        DetalleConductorScreen(correo = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.REGISTRAR_VEHICULO, a) {
        RegistrarVehiculoScreen(onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.DETALLE_VEHICULO, a) { id ->
        DetalleVehiculoScreen(patente = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.HABILITACIONES, a) { id -> HabilitacionesScreen(referencia = id, onVolver = a.volver) }
    pantalla(PantallaDemo.REGISTRAR_PAGO, a) { id ->
        RegistrarPagoScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.REGISTRAR_ADMIN, a) { RegistrarAdminScreen(onVolver = a.volver) }
    pantalla(PantallaDemo.RECUPERAR, a) { RecuperarPasswordScreen(onVolver = a.volver) }
}

fun NavGraphBuilder.grafoConductor(a: AccionesNav) {
    navigation(startDestination = PantallaDemo.MIS_VIAJES.ruta, route = Rutas.GRAFO_CONDUCTOR) {
        composable(PantallaDemo.MIS_VIAJES.ruta) { MisViajesScreen(onNavegar = a.navegar) }
        composable(PantallaDemo.PERFIL_CONDUCTOR.ruta) {
            PerfilScreen(
                pantalla = PantallaDemo.PERFIL_CONDUCTOR, onNavegar = a.navegar, onVolver = null,
                onCerrarSesion = a.cerrarSesion,
            )
        }
    }
    pantalla(PantallaDemo.DETALLE_VIAJE, a) { id ->
        DetalleViajeScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.ANTES_VENTANA, a) { id ->
        DetalleViajeScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver)
    }
    pantalla(PantallaDemo.NAVEGAR, a) { id ->
        DetalleViajeScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver, abrirNavegacion = true)
    }
    pantalla(PantallaDemo.SIN_CONEXION, a) { id ->
        DetalleViajeScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver, escenario = EscenarioDemo.SIN_CONEXION)
    }
    pantalla(PantallaDemo.CONFLICTO, a) { id ->
        DetalleViajeScreen(folio = id, onNavegar = a.navegar, onVolver = a.volver, escenario = EscenarioDemo.CONFLICTO)
    }
    pantalla(PantallaDemo.CONFIRMAR_INICIO, a) { id -> ConfirmarInicioScreen(folio = id, onVolver = a.volver) }
}
