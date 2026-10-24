package cl.belmontespa.myapplication.ui.administrador

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.local.repository.AuthLocalRepository
import cl.belmontespa.myapplication.data.local.repository.SoporteLocal
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.AuthFalso
import cl.belmontespa.myapplication.ui.navigation.BelmonteApp
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.pantallas.PantallaT06
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import dagger.hilt.android.testing.*
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

@HiltAndroidTest
class HU28ScreenTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @BindValue @JvmField val auth: AuthRepository = AuthFalso()
    @Inject lateinit var sesiones: SesionStore
    @Inject lateinit var local: SoporteLocal
    private val correo = "nuevo-admin@test.cl"
    @Before fun preparar() {
        hilt.inject()
        runBlocking { sesiones.guardar(Sesion("prueba", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, local.clock.millis() + 3600000)) }
    }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun campo(id: Int) = compose.onNode(hasText(texto(id)) and hasSetTextAction())
    private fun accion(id: Int) = compose.onNode(hasText(texto(id)) and hasClickAction())
    private fun esperar(id: Int) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty() }
    private fun abrir(escala: Float = 1f) {
        compose.activity.runOnUiThread { compose.activity.setContent {
            var ruta by remember { mutableStateOf(PantallaDemo.PERFIL_ADMIN) }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                BelmonteTheme { key(ruta) { PantallaT06(ruta, onNavegar = { p, _ -> ruta = p },
                    onVolver = { ruta = PantallaDemo.PERFIL_ADMIN }, onCerrarSesion = {}) } }
            }
        } }
        esperar(R.string.t06_registrar_admin); accion(R.string.t06_registrar_admin).performScrollTo().performClick()
        esperar(R.string.admin_alta_ayuda)
    }
    private fun formulario(correo: String = this.correo) {
        campo(R.string.campo_nombre).performScrollTo().performTextReplacement("Nueva Administradora")
        campo(R.string.campo_correo).performScrollTo().performTextReplacement(correo)
        campo(R.string.campo_telefono).performScrollTo().performTextReplacement("9 5555-0101")
        campo(R.string.admin_password_inicial).performScrollTo().performTextReplacement("Inicial12345")
    }
    @Test fun altaDesdePerfilAl200PorCientoYPrimerLoginExigeCambioAntesDeAbrirElInicio() {
        abrir(2f); formulario(correo.uppercase()); accion(R.string.accion_guardar).performScrollTo().performClick()
        esperar(R.string.admin_alta_guardada)
        campo(R.string.admin_password_inicial).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        runBlocking {
            val creador = sesiones.estado.first() as EstadoSesion.Autenticada
            assertEquals("recepcion@belmontespa.cl", creador.sesion.correo)
            assertNull(local.usuario(correo).passwordSha256)
            val authLocal = AuthLocalRepository(local.db.usuarioDao(), sesiones, local.clock, local)
            assertTrue((authLocal.iniciarSesion(correo, "Inicial12345") as Resultado.Exito).valor is EstadoSesion.Temporal)
        }
        compose.activity.runOnUiThread { compose.activity.setContent {
            val sesion by sesiones.estado.collectAsState(initial = null)
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                BelmonteTheme { BelmonteApp(sesion, onCerrarSesion = { runBlocking { sesiones.borrar() } }) }
            }
        } }
        esperar(R.string.aviso_cambio_password_obligatorio)
        campo(R.string.campo_password_actual).performScrollTo().performTextInput("Inicial12345")
        campo(R.string.campo_password_nueva).performScrollTo().performTextInput("NuevaClave987")
        campo(R.string.campo_confirmacion).performScrollTo().performTextInput("NuevaClave987")
        accion(R.string.accion_guardar).performScrollTo().performClick()
        compose.waitUntil(15_000) { runBlocking { sesiones.estado.first() is EstadoSesion.Autenticada } }
        runBlocking {
            assertNull(local.usuario(correo).passwordTemporalSha256)
            assertEquals(correo, (sesiones.estado.first() as EstadoSesion.Autenticada).sesion.correo)
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(R.string.aviso_cambio_password_obligatorio)).fetchSemanticsNodes().isEmpty() }
    }
    @Test fun erroresDeCamposYCorreoRepetidoConservanDatosParaCorregirSinSobrescribir() {
        abrir()
        campo(R.string.campo_nombre).performTextInput("A"); campo(R.string.campo_correo).performTextInput("invalido")
        campo(R.string.admin_password_inicial).performScrollTo().performTextInput("abcdefgh")
        accion(R.string.accion_guardar).performScrollTo().performClick()
        compose.onNodeWithTag("t06_error").performScrollTo().assertIsDisplayed()
        val original = runBlocking { local.usuario("recepcion@belmontespa.cl") }
        formulario(original.correo); accion(R.string.accion_guardar).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("El correo ya está registrado.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(original, runBlocking { local.usuario(original.correo) })
        campo(R.string.campo_correo).performScrollTo().performTextReplacement(correo)
        accion(R.string.accion_guardar).performScrollTo().performClick(); esperar(R.string.admin_alta_guardada)
        assertEquals("ADMINISTRADOR", runBlocking { local.usuario(correo).rol })
    }
}
