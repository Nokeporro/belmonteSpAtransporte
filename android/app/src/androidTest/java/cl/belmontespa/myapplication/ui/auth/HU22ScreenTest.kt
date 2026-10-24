package cl.belmontespa.myapplication.ui.auth

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.local.Hashes
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
class HU22ScreenTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @BindValue @JvmField val auth: AuthRepository = AuthFalso()
    @Inject lateinit var sesiones: SesionStore
    @Inject lateinit var local: SoporteLocal
    private val correo = "recepcion@belmontespa.cl"
    @Before fun preparar() { hilt.inject() }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun campo(id: Int) = compose.onNode(hasText(texto(id)) and hasSetTextAction())
    private fun accion(id: Int) = compose.onNode(hasText(texto(id)) and hasClickAction())
    private fun esperar(id: Int) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty() }
    private fun abrirRecuperacion() {
        compose.activity.runOnUiThread { compose.activity.setContent { BelmonteTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                PantallaT06(PantallaDemo.RECUPERAR, onNavegar = { _, _ -> }, onVolver = {}, onCerrarSesion = {})
            }
        } } }
        esperar(R.string.campo_correo)
    }
    private fun abrirCambio(escala: Float = 1f) {
        runBlocking {
            val u = local.usuario(correo)
            local.db.usuarioDao().guardar(u.copy(passwordSha256 = Hashes.password("Normal1234"),
                passwordTemporalSha256 = Hashes.password("Temporal1234"), passwordTemporalExpira = local.clock.millis() + 1800000))
            val localAuth = AuthLocalRepository(local.db.usuarioDao(), sesiones, local.clock, local)
            assertTrue((localAuth.iniciarSesion(correo, "Temporal1234") as Resultado.Exito).valor is EstadoSesion.Temporal)
        }
        compose.activity.runOnUiThread { compose.activity.setContent {
            val sesion by sesiones.estado.collectAsState(initial = null)
            BelmonteTheme { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                BelmonteApp(sesion, onCerrarSesion = { runBlocking { sesiones.borrar() } })
            } }
        } }
        esperar(R.string.aviso_cambio_password_obligatorio)
    }
    @Test fun recuperacionValidaElCorreoYLaConfirmacionEsGenericaAl200PorCiento() {
        abrirRecuperacion()
        campo(R.string.campo_correo).performTextInput("invalido")
        accion(R.string.login_recuperar_password).performScrollTo().performClick()
        compose.onNodeWithTag("t06_error").performScrollTo().assertIsDisplayed()
        for (valor in listOf(correo, "desconocido@test.cl")) {
            campo(R.string.campo_correo).performScrollTo().performTextReplacement(valor)
            accion(R.string.login_recuperar_password).performScrollTo().performClick(); esperar(R.string.aviso_recuperacion_enviada)
            accion(R.string.login_recuperar_password).assertIsNotEnabled()
            accion(R.string.login_iniciar_sesion).performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun cambioConTemporalAl200PorCientoValidaCamposYAbreElInicioSoloAlGuardar() {
        abrirCambio(2f)
        accion(R.string.accion_guardar).performScrollTo().performClick()
        compose.onNodeWithTag("t06_error").performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { sesiones.estado.first() } is EstadoSesion.Temporal)
        campo(R.string.campo_password_actual).performScrollTo().performTextInput("Temporal1234")
        campo(R.string.campo_password_nueva).performScrollTo().performTextInput("NuevaClave123")
        campo(R.string.campo_confirmacion).performScrollTo().performTextInput("NuevaClave123")
        accion(R.string.accion_guardar).performScrollTo().performClick(); esperar(R.string.nav_solicitudes)
        assertTrue(runBlocking { sesiones.estado.first() } is EstadoSesion.Autenticada)
        runBlocking {
            assertNull(local.usuario(correo).passwordTemporalSha256)
            assertEquals(Hashes.password("NuevaClave123"), local.usuario(correo).passwordSha256)
        }
    }
    @Test fun claveIncorrectaConservaLosCamposYNoPermiteSalirDelCambioObligatorio() {
        abrirCambio()
        campo(R.string.campo_password_actual).performTextInput("Incorrecta123")
        campo(R.string.campo_password_nueva).performTextInput("NuevaClave123")
        campo(R.string.campo_confirmacion).performScrollTo().performTextInput("NuevaClave123")
        accion(R.string.accion_guardar).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("t06_error").fetchSemanticsNodes().isNotEmpty() }
        campo(R.string.campo_password_nueva).assertTextContains("NuevaClave123")
        assertTrue(runBlocking { sesiones.estado.first() } is EstadoSesion.Temporal)
        assertEquals(Hashes.password("Normal1234"), runBlocking { local.usuario(correo).passwordSha256 })
    }
}
