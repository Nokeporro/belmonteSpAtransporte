package cl.belmontespa.myapplication.ui.administrador

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.local.repository.SoporteLocal
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.AuthFalso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.navigation.DestinoPantalla
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import dagger.hilt.android.testing.*
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

@HiltAndroidTest
class HU19ScreenTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @BindValue @JvmField val auth: AuthRepository = AuthFalso()
    @Inject lateinit var sesiones: SesionStore
    @Inject lateinit var local: SoporteLocal
    @Before fun preparar() {
        hilt.inject()
        runBlocking {
            sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, System.currentTimeMillis() + 3_600_000))
            local.db.viajeDao().guardar(local.viaje("TR-1051").copy(folio = "TR-1919", hashPago = null, estadoPago = null))
        }
    }
    private fun abrir(escala: Float = 1f) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                var ruta by remember { mutableStateOf(PantallaDemo.DETALLE_ADMIN to "TR-1919") }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta.first, ruta.second, onNavegar = { p, id -> ruta = p to id },
                                onVolver = { ruta = PantallaDemo.DETALLE_ADMIN to "TR-1919" }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(texto(R.string.accion_registrar_pago)) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        registrar()
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(texto(R.string.campo_monto)) and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun registrar() = compose.onNode(hasText(texto(R.string.accion_registrar_pago)) and hasClickAction()).performScrollTo().performClick()
    private fun monto() = compose.onNode(hasText(texto(R.string.campo_monto)) and hasSetTextAction())
    private fun elegir(id: Int, opcion: String) {
        compose.onNode(hasText(texto(id)) and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText(opcion) and hasClickAction() and hasAnyAncestor(isDialog())).performScrollTo().performClick()
    }
    private fun esperarDetalle() = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(texto(R.string.pago_fecha_pendiente)).fetchSemanticsNodes().isNotEmpty()
    }
    @Test fun registraDesdeElViajeYAbreElDetalleDelPagoPendiente() {
        abrir()
        monto().performTextInput("45001")
        elegir(R.string.campo_banco, "BancoEstado")
        elegir(R.string.campo_medio_pago, "Efectivo")
        elegir(R.string.campo_tipo_recibo, "Factura")
        registrar(); esperarDetalle()
        compose.onNodeWithText("TR-1919").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.pago_fecha_pendiente)).performScrollTo().assertIsDisplayed()
        runBlocking {
            val pago = local.db.pagoDao().obtenerDelViaje("TR-1919")!!
            assertEquals(45001, pago.monto); assertEquals("PENDIENTE", pago.estadoPago)
            assertEquals(12, pago.codSbif); assertEquals("EFECTIVO", pago.medioPago); assertEquals("FACTURA", pago.tipoRecibo)
            assertTrue(pago.hash.matches(Regex("[0-9a-f]{32}"))); assertNull(pago.fechaPago)
        }
        compose.onNodeWithContentDescription(texto(R.string.accion_volver)).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(R.string.campo_pago)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(texto(R.string.accion_registrar_pago)) and hasClickAction()).assertCountEquals(0)
    }
    @Test fun erroresAl200PorCientoConservanElMontoYPermitenCorregir() {
        abrir(2f)
        monto().performScrollTo().performTextInput("0"); registrar()
        compose.onNodeWithTag("pantalla_error").performScrollTo().assertIsDisplayed()
        assertNull(runBlocking { local.db.pagoDao().obtenerDelViaje("TR-1919") })
        monto().performScrollTo().assertTextContains("0")
        elegir(R.string.campo_banco, "BancoEstado")
        monto().performScrollTo().performTextReplacement("1")
        registrar(); esperarDetalle()
        assertEquals(1, runBlocking { local.db.pagoDao().obtenerDelViaje("TR-1919")!!.monto })
    }
}
