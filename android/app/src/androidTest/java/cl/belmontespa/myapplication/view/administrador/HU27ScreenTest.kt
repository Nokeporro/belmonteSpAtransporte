package cl.belmontespa.myapplication.view.administrador

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.navigation.DestinoPantalla
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class HU27ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    private val correo = "rperez@andesconsult.cl"
    @Before fun preparar() {
        runBlocking { sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, local.clock.millis() + 3600000)) }
    }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun cantidad(numero: Int) = compose.activity.getString(R.string.cuentas_pendientes_cantidad, numero)
    private fun accion(id: Int) = compose.onNode(hasText(texto(id)) and hasClickAction())
    private fun esperar(id: Int) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty() }
    private fun abrir(escala: Float = 1f) {
        compose.activity.runOnUiThread { compose.activity.setContent {
            var ruta by remember { mutableStateOf(PantallaDemo.SOLICITUDES) }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                BelmonteTheme { key(ruta) {
                    DestinoPantalla(ruta, onNavegar = { p, _ -> ruta = p }, onVolver = { ruta = PantallaDemo.SOLICITUDES }, onCerrarSesion = {})
                } }
            }
        } }
        compose.waitUntil(15_000) { compose.onAllNodesWithText(cantidad(1)).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun abrirCuentas() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(cantidad(1)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(cantidad(1)).performScrollTo().performClick()
        esperar(R.string.accion_aprobar)
    }
    private fun confirmar() = compose.onNode(hasText(texto(R.string.accion_confirmar)) and hasAnyAncestor(isDialog())).performClick()
    @Test fun aprobarAl200PorCientoActualizaLaListaYElContadorAlVolverSinCambiarFiltro() {
        abrir(2f)
        compose.onNode(hasText(texto(R.string.estado_viaje_asignado)) and hasClickAction()).performScrollTo().performClick()
        abrirCuentas()
        compose.onNodeWithText(correo).performScrollTo().assertIsDisplayed()
        accion(R.string.accion_aprobar).performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.cuenta_aprobar_titulo)).assertIsDisplayed()
        confirmar(); esperar(R.string.cuenta_revision_aprobada)
        esperar(R.string.vacio_cuentas_por_aprobar)
        assertEquals("APROBADO", runBlocking { local.usuario(correo).estadoSolicitante })
        compose.onNodeWithContentDescription(texto(R.string.accion_volver)).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText(cantidad(0)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(texto(R.string.estado_viaje_asignado)) and isSelected()).assertExists()
    }
    @Test fun rechazarPideConfirmacionYCancelarElDialogoNoModificaLaCuenta() {
        abrir(); abrirCuentas()
        accion(R.string.accion_rechazar).performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.cuenta_rechazar_titulo)).assertIsDisplayed()
        compose.onNode(hasText(texto(R.string.accion_cancelar)) and hasAnyAncestor(isDialog())).performClick()
        assertEquals("PENDIENTE", runBlocking { local.usuario(correo).estadoSolicitante })
        accion(R.string.accion_rechazar).performScrollTo().performClick(); confirmar(); esperar(R.string.cuenta_revision_rechazada)
        esperar(R.string.vacio_cuentas_por_aprobar)
        assertEquals("RECHAZADO", runBlocking { local.usuario(correo).estadoSolicitante })
        compose.onAllNodes(hasText(texto(R.string.accion_aprobar)) and hasClickAction()).assertCountEquals(0)
    }
    @Test fun otraRevisionMientrasSeConfirmaMuestraConflictoYRecargaLaLista() {
        abrir(); abrirCuentas(); accion(R.string.accion_aprobar).performScrollTo().performClick()
        runBlocking { local.db.usuarioDao().guardar(local.usuario(correo).copy(estadoSolicitante = "RECHAZADO")) }
        confirmar()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_error").fetchSemanticsNodes().isNotEmpty() }
        esperar(R.string.vacio_cuentas_por_aprobar)
        compose.onAllNodesWithText(texto(R.string.cuenta_revision_aprobada)).assertCountEquals(0)
        accion(R.string.accion_reintentar).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_error").fetchSemanticsNodes().isEmpty() }
        assertEquals("RECHAZADO", runBlocking { local.usuario(correo).estadoSolicitante })
    }
}
