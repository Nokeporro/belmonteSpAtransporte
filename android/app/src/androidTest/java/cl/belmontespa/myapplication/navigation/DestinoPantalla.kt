package cl.belmontespa.myapplication.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

/**
 * Ayuda de las pruebas instrumentadas: dibuja una pantalla del catálogo [PantallaDemo] sin el NavHost, con una
 * navegación falsa. La app real las conecta en NavGrafos.kt (una ruta por pantalla); aquí se repite la elección de la
 * pantalla para probar cada una por separado. Una variante comparte pantalla con otra cuando solo cambia un aviso.
 * `raiz` es verdadero en las pantallas de la barra inferior, que no tienen flecha para volver.
 */
@Composable
fun DestinoPantalla(
    pantalla: PantallaDemo,
    referencia: String = "",
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    raiz: Boolean = false,
) {
    when (pantalla) {
        // La variante de sesión vencida es el Login con su aviso, como muestran los mockups.
        PantallaDemo.LOGIN, PantallaDemo.LOGIN_DEMO, PantallaDemo.SESION_EXPIRADA -> Box(
            Modifier.fillMaxSize().testTag("pantalla_contenido"),
        ) {
            LoginScreen(
                onCrearCuenta = { onNavegar(PantallaDemo.REGISTRO, "") },
                onAbordarViaje = { onNavegar(PantallaDemo.ABORDAR, "") },
                correoInicial = referencia.takeIf { it.contains('@') }.orEmpty(),
                mostrarSesionExpirada = pantalla == PantallaDemo.SESION_EXPIRADA,
                modoDemoForzado = if (pantalla == PantallaDemo.LOGIN_DEMO) true else null,
            )
        }
        PantallaDemo.SOLICITUDES -> SolicitudesScreen(onNavegar = onNavegar)
        PantallaDemo.MIS_VIAJES -> MisViajesScreen(onNavegar = onNavegar)
        // «Antes de la ventana» y «Abrir navegación» son este detalle, el segundo con la hoja «Abrir con» encima.
        PantallaDemo.DETALLE_VIAJE, PantallaDemo.ANTES_VENTANA, PantallaDemo.NAVEGAR -> DetalleViajeScreen(
            folio = referencia, onNavegar = onNavegar, onVolver = onVolver,
            abrirNavegacion = pantalla == PantallaDemo.NAVEGAR,
        )
        // «Sin conexión» y «Cambio en conflicto» son este mismo detalle con los avisos del escenario.
        PantallaDemo.SIN_CONEXION, PantallaDemo.CONFLICTO -> DetalleViajeScreen(
            folio = referencia, onNavegar = onNavegar, onVolver = onVolver,
            escenario = if (pantalla == PantallaDemo.SIN_CONEXION) EscenarioDemo.SIN_CONEXION else EscenarioDemo.CONFLICTO,
        )
        PantallaDemo.INICIO -> InicioScreen()
        PantallaDemo.CONFIRMAR_INICIO -> ConfirmarInicioScreen(folio = referencia, onVolver = onVolver)
        // «Escanear código QR» es el lector de Google Code Scanner: la misma pantalla de «Abordar viaje».
        PantallaDemo.ABORDAR, PantallaDemo.ESCANEAR -> AbordarScreen(
            pantalla = pantalla, onNavegar = onNavegar, onVolver = onVolver,
        )
        PantallaDemo.CONFIRMAR_ABORDAJE -> ConfirmarAbordajeScreen(
            argumento = referencia, onNavegar = onNavegar, onVolver = onVolver,
        )
        PantallaDemo.ABORDADO -> AbordadoScreen(hora = referencia, onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.ASIGNAR -> AsignarScreen(folio = referencia, onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.CANCELAR_VIAJE -> CancelarViajeScreen(folio = referencia, onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.REGISTRO -> RegistroScreen(onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.RECUPERAR -> RecuperarPasswordScreen(onVolver = onVolver)
        PantallaDemo.CAMBIAR_PASSWORD, PantallaDemo.CAMBIO_OBLIGATORIO -> CambiarPasswordScreen(
            pantalla = pantalla, onVolver = onVolver, onCerrarSesion = onCerrarSesion,
        )
        PantallaDemo.PERFIL_SOLICITANTE, PantallaDemo.PERFIL_ADMIN, PantallaDemo.PERFIL_CONDUCTOR -> PerfilScreen(
            pantalla = pantalla, onNavegar = onNavegar, onVolver = onVolver.takeUnless { raiz },
            onCerrarSesion = onCerrarSesion,
        )
        PantallaDemo.NOTIFICACION -> NotificacionScreen(onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.MIS_SOLICITUDES -> MisSolicitudesScreen(onNavegar = onNavegar)
        PantallaDemo.NUEVA_SOLICITUD -> NuevaSolicitudScreen(onNavegar = onNavegar, onVolver = onVolver)
        // «Solicitud sin pasajeros» es el mismo detalle, con el aviso de que faltan pasajeros.
        PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.SOLICITUD_SIN_PASAJEROS -> DetalleSolicitudScreen(
            pantalla = pantalla, folio = referencia, onNavegar = onNavegar, onVolver = onVolver,
        )
        PantallaDemo.AGREGAR_PASAJERO -> AgregarPasajeroScreen(folio = referencia, onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.CONFIRMAR_CANCELACION -> ConfirmarCancelacionScreen(
            folio = referencia, onNavegar = onNavegar, onVolver = onVolver,
        )
        PantallaDemo.SOLICITUD_CANCELADA -> SolicitudCanceladaScreen(
            folio = referencia, onNavegar = onNavegar, onVolver = onVolver,
        )
        PantallaDemo.CUENTAS_PENDIENTES -> CuentasPendientesScreen(onVolver = onVolver)
        // «Solicitud sin pasajeros» es el mismo detalle, con el aviso de que faltan pasajeros.
        PantallaDemo.DETALLE_ADMIN, PantallaDemo.SIN_PASAJEROS_ADMIN -> DetalleSolicitudAdminScreen(
            pantalla = pantalla, folio = referencia, onNavegar = onNavegar, onVolver = onVolver,
        )
        PantallaDemo.CONDUCTORES -> ConductoresScreen(onNavegar = onNavegar)
        PantallaDemo.REGISTRAR_CONDUCTOR -> RegistrarConductorScreen(onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.DETALLE_CONDUCTOR -> DetalleConductorScreen(correo = referencia, onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.VEHICULOS -> VehiculosScreen(onNavegar = onNavegar)
        PantallaDemo.REGISTRAR_VEHICULO -> RegistrarVehiculoScreen(onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.DETALLE_VEHICULO -> DetalleVehiculoScreen(patente = referencia, onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.HABILITACIONES -> HabilitacionesScreen(referencia = referencia, onVolver = onVolver)
        PantallaDemo.REGISTRAR_PAGO -> RegistrarPagoScreen(folio = referencia, onNavegar = onNavegar, onVolver = onVolver)
        PantallaDemo.DETALLE_PAGO -> DetallePagoScreen(hash = referencia, onVolver = onVolver)
        PantallaDemo.REGISTRAR_ADMIN -> RegistrarAdminScreen(onVolver = onVolver)
    }
}
