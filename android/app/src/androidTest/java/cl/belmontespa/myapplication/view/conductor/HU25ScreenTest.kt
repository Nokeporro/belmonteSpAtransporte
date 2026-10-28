package cl.belmontespa.myapplication.view.conductor

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
import cl.belmontespa.myapplication.repository.viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class HU25ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    @Before fun preparar() {
        runBlocking { sesiones.guardar(Sesion("prueba", "ireyes@belmontespa.cl", Rol.CONDUCTOR, System.currentTimeMillis() + 3_600_000)) }
    }
    private fun abrir(folio: String, escala: Float = 1f) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                var ruta by remember { mutableStateOf(PantallaDemo.DETALLE_VIAJE to folio) }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta.first, ruta.second, onNavegar = { p, id -> ruta = p to id }, onVolver = { ruta = PantallaDemo.MIS_VIAJES to "" }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_contenido").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun abrirDialogo() {
        compose.onNodeWithText(texto(R.string.accion_rechazar)).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun campo() = compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
    private fun confirmar() = compose.onNode(hasText(texto(R.string.accion_rechazar)) and hasClickAction() and hasAnyAncestor(isDialog()))
    private fun esperarTexto(texto: String) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    @Test fun rechazaConUnMotivoValidoYVuelveAMisViajesConElViajeEnPendiente() {
        abrir("TR-1042")
        abrirDialogo()

        campo().performTextInput("abc")
        confirmar().performClick()
        compose.onNodeWithText("Debe tener entre 5 y 200 caracteres.").assertIsDisplayed()
        runBlocking { assertEquals("ASIGNADO", local.viaje("TR-1042").estado) }

        campo().performTextReplacement("  Falla del vehículo  ")
        compose.onNodeWithText("Debe tener entre 5 y 200 caracteres.").assertDoesNotExist()
        confirmar().performClick()

        esperarTexto(texto(R.string.t06_mis_viajes))
        compose.onNodeWithText(texto(R.string.accion_rechazar)).assertDoesNotExist()
        runBlocking {
            val viaje = local.viaje("TR-1042")
            assertEquals("PENDIENTE", viaje.estado); assertEquals("Falla del vehículo", viaje.motivo)
            assertNull(viaje.correoConductor); assertNull(viaje.patenteMovil); assertNull(viaje.fhAsignacion)
            assertTrue(local.db.viajePasajeroDao().obtenerDelViaje("TR-1042").all { it.fhAbordaje == null })
        }
    }

    @Test fun cancelarElDialogoNoRechazaYConservaLoEscrito() {
        abrir("TR-1042")
        abrirDialogo()
        campo().performTextInput("Falla del vehículo")
        compose.onNode(hasText(texto(R.string.accion_cancelar)) and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(15_000) { compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isEmpty() }
        runBlocking { assertEquals("ASIGNADO", local.viaje("TR-1042").estado) }
        abrirDialogo()
        campo().assertTextContains("Falla del vehículo")
    }

    @Test fun elDialogoAlcanzaSusBotonesConTextoAl200PorCiento() {
        abrir("TR-1042", escala = 2f)
        abrirDialogo()

        campo().performTextInput("abc")
        confirmar().assertIsDisplayed().performClick()

        compose.onNodeWithText("Debe tener entre 5 y 200 caracteres.").assertIsDisplayed()
        confirmar().assertIsDisplayed()
        compose.onNode(hasText(texto(R.string.accion_cancelar)) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        runBlocking { assertEquals("ASIGNADO", local.viaje("TR-1042").estado) }
    }
}
