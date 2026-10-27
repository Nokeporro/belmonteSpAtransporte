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
import cl.belmontespa.myapplication.repository.viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** «Cancelar viaje» (HU24) de punta a punta sobre Room: del formulario al detalle con el resultado sobre el pago. */
class HU24ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    @Before fun preparar() {
        runBlocking { sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, System.currentTimeMillis() + 3_600_000)) }
    }
    private fun abrir(folio: String, escala: Float = 1f) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                var ruta by remember { mutableStateOf(PantallaDemo.CANCELAR_VIAJE to folio) }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta.first, ruta.second, onNavegar = { p, id -> ruta = p to id }, onVolver = { ruta = PantallaDemo.DETALLE_ADMIN to folio }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_contenido").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun campo(etiqueta: Int) = compose.onNode(hasText(compose.activity.getString(etiqueta)) and hasSetTextAction())
    private fun confirmar() = compose.onNodeWithText(compose.activity.getString(R.string.accion_confirmar)).performScrollTo().performClick()
    private fun esperarTexto(texto: String) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    @Test fun cancelaUnViajeVencidoConElMontoQueFijaYElDetalleMuestraElResultado() {
        abrir("TR-1038")
        // El monto llega precargado con el total del pago.
        campo(R.string.campo_reembolso).assertTextContains("52000")
        campo(R.string.campo_motivo).performTextInput("Nadie lo atendió")
        campo(R.string.campo_reembolso).performTextReplacement("20000")
        confirmar()

        // El resultado sobre el pago solo existe en el detalle: el motivo también está escrito en el formulario.
        esperarTexto(compose.activity.getString(R.string.campo_monto_reembolsado))
        compose.onNodeWithText(compose.activity.getString(R.string.campo_monto_reembolsado)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("\$20.000").performScrollTo().assertIsDisplayed()
        runBlocking {
            val viaje = local.viaje("TR-1038")
            assertEquals("CANCELADO", viaje.estado); assertEquals("Nadie lo atendió", viaje.motivo)
            val pago = local.db.pagoDao().obtener(viaje.hashPago!!)!!
            assertEquals("REEMBOLSADO", pago.estadoPago); assertEquals(20_000, pago.montoReembolsado)
        }
    }

    @Test fun conElViajeAlDiaNoPideMontoYReembolsaTodo() {
        abrir("TR-1042")
        campo(R.string.campo_reembolso).assertDoesNotExist()
        campo(R.string.campo_motivo).performTextInput("Falla del vehículo")
        confirmar()

        esperarTexto(compose.activity.getString(R.string.campo_monto_reembolsado))
        // El detalle del pago muestra el monto y, con el mismo valor, el monto reembolsado.
        compose.onAllNodesWithText("\$48.000").assertCountEquals(2)
        runBlocking {
            val pago = local.db.pagoDao().obtener(local.viaje("TR-1042").hashPago!!)!!
            assertEquals("REEMBOLSADO", pago.estadoPago); assertEquals(48_000, pago.montoReembolsado)
        }
    }

    @Test fun unMontoFueraDeRangoMuestraSuErrorYNoCancela() {
        abrir("TR-1038")
        campo(R.string.campo_motivo).performTextInput("Nadie lo atendió")
        campo(R.string.campo_reembolso).performTextReplacement("52001")
        confirmar()

        compose.onNodeWithText("Debe estar entre \$0 y \$52.000.").performScrollTo().assertIsDisplayed()
        runBlocking { assertEquals("PENDIENTE", local.viaje("TR-1038").estado) }
    }

    @Test fun elFormularioAlcanzaSusBotonesConTextoAl200PorCiento() {
        abrir("TR-1038", escala = 2f)
        campo(R.string.campo_motivo).performScrollTo().performTextInput("1234")
        compose.onNodeWithText(compose.activity.getString(R.string.accion_confirmar)).performScrollTo().assertIsDisplayed().performClick()

        compose.onNodeWithText("Debe tener entre 5 y 200 caracteres.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.accion_volver)).performScrollTo().assertIsDisplayed()
        runBlocking { assertEquals("PENDIENTE", local.viaje("TR-1038").estado) }
    }
}
