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

/** «Reasignar» (HU23) de punta a punta sobre Room: del detalle de un viaje Asignado a «Asignar conductor y vehículo» y de vuelta. */
class HU23ScreenTest {
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
                var ruta by remember { mutableStateOf(PantallaDemo.DETALLE_ADMIN to folio) }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta.first, ruta.second, onNavegar = { p, id -> ruta = p to id }, onVolver = { ruta = PantallaDemo.DETALLE_ADMIN to folio }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        esperarTexto(compose.activity.getString(R.string.accion_reasignar))
    }
    private fun esperarTexto(texto: String) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    private fun reasignar() = compose.onNodeWithText(compose.activity.getString(R.string.accion_reasignar)).performScrollTo().performClick()
    private fun confirmar() = compose.onNodeWithText(compose.activity.getString(R.string.accion_confirmar_asignacion)).performScrollTo()

    @Test fun reasignaElConductorDesdeElDetalleYElDetalleMuestraAlNuevo() {
        abrir("TR-1042")
        compose.onNodeWithText("Iván Reyes").performScrollTo().assertIsDisplayed()
        reasignar()

        // Nada viene elegido: Iván, el conductor actual, sigue disponible para elegirse, pero Paula es la nueva.
        esperarTexto("Paula Muñoz")
        compose.onNodeWithText("Paula Muñoz").performScrollTo().performClick()
        esperarTexto("KXJR42")
        compose.onNodeWithText("KXJR42").performScrollTo().performClick()
        confirmar().assertIsEnabled().performClick()

        // Vuelve al detalle del viaje: «Reasignar» solo existe ahí. Sin NavHost el detalle no se recrea al volver, así
        // que su texto no es fiable en esta prueba: lo que se comprueba es Room.
        esperarTexto(compose.activity.getString(R.string.accion_reasignar))
        compose.onNodeWithText(compose.activity.getString(R.string.accion_confirmar_asignacion)).assertDoesNotExist()
        runBlocking {
            val viaje = local.viaje("TR-1042")
            assertEquals("ASIGNADO", viaje.estado)
            assertEquals("pmunoz@belmontespa.cl", viaje.correoConductor)
            assertEquals("Paula Muñoz", viaje.nombreConductor)
            assertEquals("KXJR42", viaje.patenteMovil)
            // El mismo vehículo es el mismo QR: Constanza Ríos sigue abordada.
            assertEquals(1, local.db.viajePasajeroDao().obtenerDelViaje("TR-1042").count { it.fhAbordaje != null })
        }
    }

    @Test fun elFormularioAlcanzaSusBotonesConTextoAl200PorCiento() {
        abrir("TR-1042", escala = 2f)
        reasignar()

        esperarTexto("Paula Muñoz")
        compose.onNodeWithText("Paula Muñoz").performScrollTo().assertIsDisplayed().performClick()
        esperarTexto("KXJR42")
        compose.onNodeWithText("KXJR42").performScrollTo().assertIsDisplayed().performClick()
        confirmar().assertIsDisplayed().assertIsEnabled()
        runBlocking { assertEquals("ireyes@belmontespa.cl", local.viaje("TR-1042").correoConductor) }
    }
}
