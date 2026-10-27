package cl.belmontespa.myapplication.view.administrador

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
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

class HU15ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    @Before fun preparar() {
        runBlocking { sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, System.currentTimeMillis() + 3_600_000)) }
    }
    private fun abrir(escala: Float = 1f, inicio: PantallaDemo = PantallaDemo.REGISTRAR_CONDUCTOR) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                var ruta by remember { mutableStateOf(inicio to "") }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta.first, ruta.second, onNavegar = { p, id -> ruta = p to id }, onVolver = { ruta = PantallaDemo.CONDUCTORES to "" }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_contenido").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun campo(etiqueta: Int) = compose.onNode(hasText(compose.activity.getString(etiqueta)) and hasSetTextAction())
    private fun guardar() = compose.onNodeWithText(compose.activity.getString(R.string.accion_guardar)).performScrollTo().performClick()
    @Test fun registraDesdeElFormularioYEditaEnElDetalleConRoom() {
        abrir(inicio = PantallaDemo.CONDUCTORES)
        compose.onNodeWithText(compose.activity.getString(R.string.t06_registrar_conductor)).performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(compose.activity.getString(R.string.campo_nombre)) and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        campo(R.string.campo_nombre).performTextInput("Ana Conductora")
        campo(R.string.campo_correo).performTextInput("ANA@DEMO.CL")
        campo(R.string.campo_telefono).performTextInput("9 5555-0101")
        campo(R.string.campo_password).performTextInput("Clave12345")
        guardar()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ana@demo.cl").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("ana@demo.cl").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.conductor_sin_habilitaciones)).performScrollTo().assertIsDisplayed()
        campo(R.string.campo_nombre).performScrollTo().performTextReplacement("Ana Editada")
        guardar()
        compose.waitUntil(15_000) { compose.onAllNodesWithText(compose.activity.getString(R.string.aviso_guardado)).fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            val usuario = local.usuario("ana@demo.cl")
            assertEquals("Ana Editada", usuario.nombre); assertEquals("+56955550101", usuario.telefono)
            assertTrue(usuario.activo); assertEquals("DISPONIBLE", usuario.estadoConductor)
        }
        campo(R.string.campo_password_nueva).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.accion_volver)).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Ana Editada").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ana Editada").performScrollTo().assertIsDisplayed()
    }
    @Test fun erroresVisiblesYFormularioAlcanzableConTextoAl200PorCiento() {
        abrir(2f)
        campo(R.string.campo_nombre).performScrollTo().performTextInput("A")
        campo(R.string.campo_correo).performScrollTo().performTextInput("invalido")
        campo(R.string.campo_password).performScrollTo().performTextInput("abcdefgh")
        guardar()
        compose.onNodeWithTag("pantalla_error").performScrollTo().assertIsDisplayed()
        campo(R.string.campo_correo).performScrollTo().assertTextContains("invalido")
        compose.onNodeWithText(compose.activity.getString(R.string.accion_guardar)).performScrollTo().assertIsDisplayed()
        assertNull(runBlocking { local.db.usuarioDao().obtener("invalido") })
    }
}
