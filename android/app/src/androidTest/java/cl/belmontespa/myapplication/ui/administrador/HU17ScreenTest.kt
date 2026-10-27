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
import cl.belmontespa.myapplication.util.Formatos
import dagger.hilt.android.testing.*
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

@HiltAndroidTest
class HU17ScreenTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @BindValue @JvmField val auth: AuthRepository = AuthFalso()
    @Inject lateinit var sesiones: SesionStore
    @Inject lateinit var local: SoporteLocal
    @Before fun preparar() {
        hilt.inject()
        runBlocking { sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, System.currentTimeMillis() + 3_600_000)) }
    }
    private fun abrir(escala: Float = 1f) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                var ruta by remember { mutableStateOf(PantallaDemo.VEHICULOS to "") }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta.first, ruta.second, onNavegar = { p, id -> ruta = p to id },
                                onVolver = { ruta = PantallaDemo.VEHICULOS to "" }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        esperarTexto(R.string.t06_registrar_vehiculo)
        compose.onNodeWithText(compose.activity.getString(R.string.t06_registrar_vehiculo)).performScrollTo().performClick()
        esperarTexto(R.string.vehiculo_registro_indicacion)
    }
    private fun esperarTexto(id: Int) = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(compose.activity.getString(id)).fetchSemanticsNodes().isNotEmpty()
    }
    private fun campo(id: Int) = compose.onNode(hasText(compose.activity.getString(id)) and hasSetTextAction())
    private fun guardar() = compose.onNodeWithText(compose.activity.getString(R.string.accion_guardar)).performScrollTo().performClick()
    @Test fun altaDesdeListaMuestraDetalleConCodigoDeAbordajeYRefrescaLaListaAlVolver() {
        abrir()
        campo(R.string.campo_patente).performTextInput("zx-cv 17")
        campo(R.string.campo_capacidad).performTextInput("20")
        campo(R.string.campo_gps).performTextInput("123456")
        guardar()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ZXCV17").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("123456").performScrollTo().assertIsDisplayed()
        val movil = runBlocking { local.db.movilDao().obtener("ZXCV17")!! }
        assertEquals(20, movil.capacidad); assertEquals(123456L, movil.gpsSerial)
        assertTrue(movil.activo); assertEquals("DISPONIBLE", movil.estadoMovil)
        assertTrue(movil.codigoAbordaje.matches(Regex("[0-9A-F]{16}")))
        compose.onNodeWithText(Formatos.codigoAbordaje(movil.codigoAbordaje)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.accion_volver)).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ZXCV17").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("ZXCV17").performScrollTo().assertIsDisplayed()
    }
    @Test fun erroresYPatenteDuplicadaConservanDatosConTextoAl200PorCiento() {
        abrir(2f)
        campo(R.string.campo_patente).performScrollTo().performTextInput("ABC123")
        campo(R.string.campo_capacidad).performScrollTo().performTextInput("21")
        campo(R.string.campo_gps).performScrollTo().performTextInput("0")
        guardar()
        compose.onNodeWithTag("pantalla_error").performScrollTo().assertIsDisplayed()
        campo(R.string.campo_capacidad).performScrollTo().assertTextContains("21")
        assertNull(runBlocking { local.db.movilDao().obtener("ABC123") })
        campo(R.string.campo_patente).performScrollTo().performTextReplacement("kx-jr42")
        campo(R.string.campo_capacidad).performScrollTo().performTextReplacement("1")
        campo(R.string.campo_gps).performScrollTo().performTextClearance()
        val original = runBlocking { local.db.movilDao().obtener("KXJR42") }
        guardar()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("La patente ya está registrada.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("pantalla_error").performScrollTo().assertIsDisplayed()
        campo(R.string.campo_patente).performScrollTo().assertTextContains("kx-jr42")
        assertEquals(original, runBlocking { local.db.movilDao().obtener("KXJR42") })
        campo(R.string.campo_patente).performScrollTo().performTextReplacement("ZXCV18")
        guardar()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ZXCV18").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.vehiculo_sin_gps)).performScrollTo().assertIsDisplayed()
        assertNull(runBlocking { local.db.movilDao().obtener("ZXCV18")!!.gpsSerial })
    }
}
