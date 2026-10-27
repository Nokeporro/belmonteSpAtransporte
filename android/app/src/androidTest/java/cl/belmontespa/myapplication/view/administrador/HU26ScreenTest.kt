package cl.belmontespa.myapplication.view.administrador

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
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

class HU26ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    @Before fun preparar() {
        runBlocking { sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, local.clock.millis() + 3600000)) }
    }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun accion(id: Int) = compose.onNode(hasText(texto(id)) and hasClickAction() and !hasAnyAncestor(isDialog()))
    private fun esperar(id: Int) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty() }
    private fun abrir(pantalla: PantallaDemo, id: String, escala: Float = 1f) {
        compose.activity.runOnUiThread { compose.activity.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                BelmonteTheme { DestinoPantalla(pantalla, id, onNavegar = { _, _ -> }, onVolver = {}, onCerrarSesion = {}) }
            }
        } }
        esperar(R.string.flota_activo_ayuda)
    }
    private fun confirmar() = compose.onNode(hasText(texto(R.string.accion_confirmar)) and hasAnyAncestor(isDialog())).performClick()
    @Test fun conductorAl200PorCientoConfirmaYCancelarNoCambiaElEstado() {
        val correo = "pmunoz@belmontespa.cl"; abrir(PantallaDemo.DETALLE_CONDUCTOR, correo, 2f)
        accion(R.string.accion_desactivar).performScrollTo().performClick()
        compose.onNode(hasText(texto(R.string.accion_cancelar)) and hasAnyAncestor(isDialog())).performClick()
        assertTrue(runBlocking { local.usuario(correo).activo })
        accion(R.string.accion_desactivar).performScrollTo().performClick(); confirmar(); esperar(R.string.accion_reactivar)
        assertFalse(runBlocking { local.usuario(correo).activo })
        accion(R.string.accion_reactivar).performScrollTo().performClick(); confirmar(); esperar(R.string.accion_desactivar)
        assertTrue(runBlocking { local.usuario(correo).activo })
    }
    @Test fun vehiculoReactivaYDesactivaConservandoElQr() {
        val patente = "HJKL21"; val original = runBlocking { local.db.movilDao().obtener(patente)!! }
        abrir(PantallaDemo.DETALLE_VEHICULO, patente)
        accion(R.string.accion_reactivar).performScrollTo().performClick(); confirmar(); esperar(R.string.accion_desactivar)
        assertEquals(original.copy(activo = true), runBlocking { local.db.movilDao().obtener(patente) })
        accion(R.string.accion_desactivar).performScrollTo().performClick(); confirmar(); esperar(R.string.accion_reactivar)
        assertEquals(original, runBlocking { local.db.movilDao().obtener(patente) })
    }
    @Test fun viajeAsignadoMuestraConflictoYPermiteRecargarSinAfirmarExito() {
        val correo = "ireyes@belmontespa.cl"; abrir(PantallaDemo.DETALLE_CONDUCTOR, correo)
        accion(R.string.accion_desactivar).performScrollTo().performClick(); confirmar()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_error").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(runBlocking { local.usuario(correo).activo })
        compose.onAllNodesWithText(texto(R.string.flota_activo_guardado)).assertCountEquals(0)
        accion(R.string.accion_reintentar).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_error").fetchSemanticsNodes().isEmpty() }
        esperar(R.string.accion_desactivar)
        accion(R.string.accion_desactivar).performScrollTo().assertIsEnabled()
    }
}
