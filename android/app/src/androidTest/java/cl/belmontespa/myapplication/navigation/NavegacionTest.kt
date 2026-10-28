package cl.belmontespa.myapplication.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBackUnconditionally
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


class NavegacionTest {

    @get:Rule(order = 0)
    val entorno = EntornoPruebas()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val sesiones get() = entorno.sesiones

    @Test
    fun sinSesionAbreElLogin() {
        esperarTexto(R.string.login_iniciar_sesion)
    }

    @Test
    fun cadaRolAbreSuInicioConSuBarra() {
        esperarTexto(R.string.login_iniciar_sesion)

        entrarComo(Rol.SOLICITANTE)
        esperarTexto(R.string.nav_mis_solicitudes)
        esperarTexto(R.string.nav_perfil)

        salir()
        entrarComo(Rol.ADMINISTRADOR)
        esperarTexto(R.string.nav_solicitudes)
        esperarTexto(R.string.nav_conductores)
        esperarTexto(R.string.nav_vehiculos)

        salir()
        entrarComo(Rol.CONDUCTOR)
        esperarTexto(R.string.nav_mis_viajes)
    }

    @Test
    fun cerrarSesionVuelveAlLoginSinHistorial() {
        entrarComo(Rol.SOLICITANTE)
        esperarTexto(R.string.nav_perfil)
        compose.onNodeWithText(texto(R.string.nav_perfil)).performClick()
        esperarTexto(R.string.accion_cerrar_sesion)
        compose.onNodeWithText(texto(R.string.accion_cerrar_sesion)).performClick()
        esperarTexto(R.string.login_iniciar_sesion)

        val actividad = compose.activity
        pressBackUnconditionally()
        compose.waitUntil(5_000) { actividad.isFinishing || actividad.isDestroyed || !actividad.hasWindowFocus() }
    }

    @Test
    fun sesionTemporalAbreElCambioObligatorioDeContrasena() {
        esperarTexto(R.string.login_iniciar_sesion)
        runBlocking { sesiones.guardarTemporal(sesion(Rol.ADMINISTRADOR)) }
        esperarTexto(R.string.aviso_cambio_password_obligatorio)

        compose.onNodeWithText(texto(R.string.accion_cerrar_sesion)).performClick()
        esperarTexto(R.string.login_iniciar_sesion)
    }

    @Test
    fun perderLaSesionDesdePerfilAbreElLoginSinHistorial() {
        entrarComo(Rol.SOLICITANTE)
        esperarTexto(R.string.nav_perfil)

        compose.onNodeWithText(texto(R.string.nav_perfil)).performClick()
        esperarTexto(R.string.accion_cerrar_sesion)

        runBlocking { sesiones.borrar() }
        esperarTexto(R.string.login_iniciar_sesion)

        val actividad = compose.activity
        pressBackUnconditionally()
        compose.waitUntil(5_000) { actividad.isFinishing || actividad.isDestroyed || !actividad.hasWindowFocus() }
    }

    @Test
    fun completarCambioObligatorioAbreElInicioDelRol() {
        esperarTexto(R.string.login_iniciar_sesion)
        val sesion = sesion(Rol.ADMINISTRADOR)

        runBlocking { sesiones.guardarTemporal(sesion) }
        esperarTexto(R.string.aviso_cambio_password_obligatorio)

        runBlocking { sesiones.guardar(sesion) }
        esperarTexto(R.string.nav_solicitudes)
        esperarTexto(R.string.nav_conductores)
        esperarTexto(R.string.nav_vehiculos)

        val actividad = compose.activity
        pressBackUnconditionally()
        compose.waitUntil(5_000) { actividad.isFinishing || actividad.isDestroyed || !actividad.hasWindowFocus() }
    }

    private fun entrarComo(rol: Rol) {
        runBlocking { sesiones.guardar(sesion(rol)) }
    }

    private fun salir() {
        runBlocking { sesiones.borrar() }
        esperarTexto(R.string.login_iniciar_sesion)
    }

    private fun sesion(rol: Rol) = Sesion(
        token = "prueba",
        correo = when(rol) { Rol.SOLICITANTE -> "crios@andesconsult.cl"; Rol.ADMINISTRADOR -> "recepcion@belmontespa.cl"; Rol.CONDUCTOR -> "ireyes@belmontespa.cl" },
        rol = rol,
        expiraEn = Long.MAX_VALUE,
    )

    private fun texto(@StringRes id: Int) = compose.activity.getString(id)

    private fun esperarTexto(@StringRes id: Int) {
        val buscado = texto(id)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(buscado).fetchSemanticsNodes().isNotEmpty() }
    }
}
