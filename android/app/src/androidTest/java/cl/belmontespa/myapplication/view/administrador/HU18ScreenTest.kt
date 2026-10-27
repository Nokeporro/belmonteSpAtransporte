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
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.navigation.DestinoPantalla
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class HU18ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    private val flota by lazy { FlotaRepository(local) }
    private val correo = "hu18@demo.cl"
    @Before fun preparar() {
        runBlocking {
            sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, System.currentTimeMillis() + 3_600_000))
            assertTrue(flota.registrarConductor(NuevaCuenta("Ana HU18", correo, null, "Clave12345")) is Resultado.Exito)
            assertTrue(flota.registrarMovil(NuevoMovil("ABCD18", 8, null)) is Resultado.Exito)
            assertTrue(flota.registrarMovil(NuevoMovil("WXYZ18", 2, null)) is Resultado.Exito)
        }
    }
    private fun abrir(pantalla: PantallaDemo, id: String, escala: Float = 1f) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                var ruta by remember { mutableStateOf(pantalla to id) }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta.first, ruta.second, onNavegar = { p, clave -> ruta = p to clave },
                                onVolver = { ruta = pantalla to id }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        esperarTexto(R.string.accion_habilitaciones)
        compose.onNodeWithText(compose.activity.getString(R.string.accion_habilitaciones)).performScrollTo().performClick()
        esperarTexto(R.string.habilitaciones_ayuda)
    }
    private fun esperarTexto(id: Int) = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(compose.activity.getString(id)).fetchSemanticsNodes().isNotEmpty()
    }
    private fun elegir(etiqueta: Int, opcion: String) {
        compose.onNode(hasText(compose.activity.getString(etiqueta)) and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText(opcion) and hasClickAction() and hasAnyAncestor(isDialog())).performScrollTo().performClick()
    }
    private fun habilitar() {
        compose.onNodeWithText(compose.activity.getString(R.string.accion_habilitar)).performScrollTo().performClick()
        esperarTexto(R.string.habilitacion_guardada)
        esperarTexto(R.string.habilitaciones_estado_conductor)
    }
    @Test fun desdeVehiculoAgregaSinDuplicarYFiltraLaLista() {
        abrir(PantallaDemo.DETALLE_VEHICULO, "ABCD18")
        elegir(R.string.campo_conductor, "Ana HU18 ($correo)")
        habilitar(); habilitar()
        runBlocking { assertEquals(1, local.db.habilitacionDao().observarDelMovil("ABCD18").first().size) }
        compose.onNodeWithText(compose.activity.getString(R.string.habilitaciones_estado_conductor)).performScrollTo().assertIsDisplayed()
        elegir(R.string.habilitaciones_filtro_vehiculo, "WXYZ18")
        esperarTexto(R.string.habilitaciones_sin_resultados)
        compose.onNodeWithText(compose.activity.getString(R.string.habilitaciones_sin_resultados)).performScrollTo().assertIsDisplayed()
        elegir(R.string.habilitaciones_filtro_vehiculo, "ABCD18")
        esperarTexto(R.string.habilitaciones_estado_vehiculo)
        elegir(R.string.habilitaciones_filtro_conductor, "Ana HU18 ($correo)")
        esperarTexto(R.string.habilitaciones_estado_vehiculo)
        compose.onNodeWithText(compose.activity.getString(R.string.habilitaciones_estado_vehiculo)).performScrollTo().assertIsDisplayed()
        runBlocking { assertEquals(1, local.db.habilitacionDao().observarDelMovil("ABCD18").first().size) }
    }
    @Test fun desdeConductorConTextoAl200PorCientoYDetalleActualizadoAlVolver() {
        abrir(PantallaDemo.DETALLE_CONDUCTOR, correo, 2f)
        elegir(R.string.campo_vehiculo, "ABCD18")
        habilitar()
        compose.onNodeWithText(compose.activity.getString(R.string.habilitaciones_estado_conductor)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.accion_volver)).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ABCD18").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("ABCD18").performScrollTo().assertIsDisplayed()
        runBlocking { assertEquals(correo, local.db.habilitacionDao().observarDelMovil("ABCD18").first().single().correoConductor) }
    }
}
