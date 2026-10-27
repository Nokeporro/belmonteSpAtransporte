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
import cl.belmontespa.myapplication.model.PagoEntity
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.navigation.DestinoPantalla
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class HU20ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    private val hash = "20202020202020202020202020202020"
    private val folio = "TR-2020"
    @Before fun preparar() {
        runBlocking {
            sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, System.currentTimeMillis() + 3_600_000))
            local.db.viajeDao().guardar(local.viaje("TR-1051").copy(folio = folio, hashPago = hash, estadoPago = "PENDIENTE"))
            local.db.pagoDao().insertar(PagoEntity(hash, folio, 50001, "PENDIENTE", "BancoEstado", 12,
                "12345678-5", "TRANSFERENCIA", "BOLETA", null, null))
        }
    }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun abrir(escala: Float = 1f) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme { DestinoPantalla(PantallaDemo.DETALLE_PAGO, hash, { _, _ -> }, {}, {}) }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText(folio).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun accion(id: Int) = compose.onNode(hasText(texto(id)) and hasClickAction())
    private fun confirmar() = compose.onNode(hasText(texto(R.string.accion_confirmar)) and hasAnyAncestor(isDialog())).performClick()
    private fun esperarGuardado() = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(texto(R.string.pago_estado_guardado)).fetchSemanticsNodes().isNotEmpty()
    }
    private fun sinAcciones() {
        for (id in listOf(R.string.accion_pagar, R.string.accion_rechazar, R.string.accion_anular)) {
            compose.onAllNodes(hasText(texto(id)) and hasClickAction()).assertCountEquals(0)
        }
    }
    @Test fun anularAl200PorCientoExplicaLaCancelacionYPermiteDesistirAntesDeConfirmar() {
        abrir(2f)
        accion(R.string.accion_anular).performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.pago_confirmar_anulado)).assertIsDisplayed()
        compose.onNode(hasText(texto(R.string.accion_cancelar)) and hasAnyAncestor(isDialog())).performClick()
        assertEquals("PENDIENTE", runBlocking { local.db.pagoDao().obtener(hash)!!.estadoPago })
        accion(R.string.accion_anular).performScrollTo().performClick(); confirmar(); esperarGuardado()
        sinAcciones()
        runBlocking {
            assertEquals("ANULADO", local.db.pagoDao().obtener(hash)!!.estadoPago)
            assertNull(local.db.pagoDao().obtener(hash)!!.fechaPago); assertNull(local.db.pagoDao().obtener(hash)!!.montoReembolsado)
            assertEquals("CANCELADO", local.viaje(folio).estado)
        }
    }
    @Test fun pagarMuestraFechaYDespuesSoloPermiteAnular() {
        abrir(); accion(R.string.accion_pagar).performScrollTo().performClick(); confirmar(); esperarGuardado()
        val fecha = runBlocking { local.db.pagoDao().obtener(hash)!!.fechaPago }; assertNotNull(fecha)
        compose.onAllNodes(hasText(texto(R.string.accion_pagar)) and hasClickAction()).assertCountEquals(0)
        compose.onAllNodes(hasText(texto(R.string.accion_rechazar)) and hasClickAction()).assertCountEquals(0)
        accion(R.string.accion_anular).performScrollTo().performClick(); confirmar()
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(texto(R.string.accion_anular)) and hasClickAction()).fetchSemanticsNodes().isEmpty() }
        sinAcciones(); assertEquals(fecha, runBlocking { local.db.pagoDao().obtener(hash)!!.fechaPago })
    }
    @Test fun rechazarEsTerminalYNoCancelaElViaje() {
        abrir(); accion(R.string.accion_rechazar).performScrollTo().performClick(); confirmar(); esperarGuardado()
        sinAcciones()
        runBlocking {
            assertEquals("RECHAZADO", local.db.pagoDao().obtener(hash)!!.estadoPago)
            assertEquals("PENDIENTE", local.viaje(folio).estado)
        }
    }
    @Test fun conflictoPorCambioExternoConservaElDetalleYReintentarCargaElEstadoVigente() {
        abrir(); accion(R.string.accion_anular).performScrollTo().performClick()
        runBlocking { local.db.pagoDao().guardar(local.db.pagoDao().obtener(hash)!!.copy(estadoPago = "RECHAZADO")) }
        confirmar()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_error").fetchSemanticsNodes().isNotEmpty() }
        accion(R.string.accion_anular).assertIsNotEnabled()
        accion(R.string.accion_reintentar).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(texto(R.string.accion_anular)) and hasClickAction()).fetchSemanticsNodes().isEmpty() }
        sinAcciones(); assertEquals("PENDIENTE", runBlocking { local.viaje(folio).estado })
    }
}
