package cl.belmontespa.myapplication.view.auth

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.navigation.BelmonteApp
import cl.belmontespa.myapplication.navigation.DestinoPantalla
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.repository.AuthRepository
import cl.belmontespa.myapplication.repository.usuario
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.util.Hashes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class HU22ScreenTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val sesiones get() = entorno.sesiones
    private val local get() = entorno.local
    private val correo = "recepcion@belmontespa.cl"
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun campo(id: Int) = compose.onNode(hasText(texto(id)) and hasSetTextAction())
    private fun accion(id: Int) = compose.onNode(hasText(texto(id)) and hasClickAction())
    private fun esperar(id: Int) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty() }
    private fun abrirRecuperacion() {
        runBlocking { sesiones.guardar(Sesion("prueba", correo, Rol.ADMINISTRADOR, System.currentTimeMillis() + 3_600_000)) }
        compose.activity.runOnUiThread { compose.activity.setContent { BelmonteTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                DestinoPantalla(PantallaDemo.RECUPERAR, onNavegar = { _, _ -> }, onVolver = {}, onCerrarSesion = {})
            }
        } } }
        esperar(R.string.campo_correo_cuenta)
    }
    private fun abrirCambio(escala: Float = 1f) {
        runBlocking {
            val u = local.usuario(correo)
            local.db.usuarioDao().guardar(u.copy(passwordSha256 = Hashes.password("Normal1234"),
                passwordTemporalSha256 = Hashes.password("Temporal1234"), passwordTemporalExpira = local.clock.millis() + 1800000))
            val localAuth = AuthRepository(local)
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
    @Test fun elAdministradorGeneraLaTemporalDeUnaCuentaYSeMuestraUnaSolaVezAl200PorCiento() {
        abrirRecuperacion()
        campo(R.string.campo_correo_cuenta).performTextInput("invalido")
        accion(R.string.recuperar_generar).performScrollTo().performClick()
        compose.onNodeWithTag("pantalla_error").performScrollTo().assertIsDisplayed()
        campo(R.string.campo_correo_cuenta).performScrollTo().performTextReplacement("ireyes@belmontespa.cl")
        accion(R.string.recuperar_generar).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_error").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(texto(R.string.recuperar_resultado)).assertCountEquals(0)
        campo(R.string.campo_correo_cuenta).performScrollTo().performTextReplacement("crios@andesconsult.cl")
        accion(R.string.recuperar_generar).performScrollTo().performClick(); esperar(R.string.recuperar_resultado)
        accion(R.string.accion_copiar).performScrollTo().assertIsDisplayed()
        assertNotNull(runBlocking { local.usuario("crios@andesconsult.cl").passwordTemporalSha256 })
        campo(R.string.campo_correo_cuenta).performScrollTo().performTextReplacement("rperez@andesconsult.cl")
        compose.onAllNodesWithText(texto(R.string.recuperar_resultado)).assertCountEquals(0)
    }
    @Test fun recuperarContrasenaEnElLoginSoloIndicaPedirLaTemporalAUnAdministrador() {
        runBlocking { sesiones.borrar() }
        compose.activity.runOnUiThread { compose.activity.setContent { BelmonteTheme {
            DestinoPantalla(PantallaDemo.LOGIN, onNavegar = { _, _ -> }, onVolver = {}, onCerrarSesion = {})
        } } }
        esperar(R.string.login_recuperar_password)
        compose.onAllNodesWithText(texto(R.string.aviso_pedir_temporal_admin)).assertCountEquals(0)
        accion(R.string.login_recuperar_password).performScrollTo().performClick()
        esperar(R.string.aviso_pedir_temporal_admin)
    }
    @Test fun cambioConTemporalAl200PorCientoValidaCamposYAbreElInicioSoloAlGuardar() {
        abrirCambio(2f)
        accion(R.string.accion_guardar).performScrollTo().performClick()
        compose.onNodeWithTag("pantalla_error").performScrollTo().assertIsDisplayed()
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
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_error").fetchSemanticsNodes().isNotEmpty() }
        campo(R.string.campo_password_nueva).assertTextContains("NuevaClave123")
        assertTrue(runBlocking { sesiones.estado.first() } is EstadoSesion.Temporal)
        assertEquals(Hashes.password("Normal1234"), runBlocking { local.usuario(correo).passwordSha256 })
    }
}
