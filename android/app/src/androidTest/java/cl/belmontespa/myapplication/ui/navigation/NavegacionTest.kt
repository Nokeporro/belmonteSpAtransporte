package cl.belmontespa.myapplication.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBackUnconditionally
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.di.RepositorioModule
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import cl.belmontespa.myapplication.data.session.SesionStore

/** Flujo por rol, cambio obligatorio de contraseña e historial tras el login y el cierre de sesión. */
@HiltAndroidTest

class NavegacionTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val auth: AuthRepository = AuthFalso()

    private val falso get() = auth as AuthFalso
    @Inject lateinit var sesiones: SesionStore
    @Before fun preparar() { hilt.inject() }

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

        // Desde el Login no se vuelve al flujo autenticado: atrás cierra la app.
        val actividad = compose.activity
        pressBackUnconditionally()
        // Android 12 o superior puede enviar la actividad raíz al fondo.
        compose.waitUntil(5_000) { actividad.isFinishing || actividad.isDestroyed || !actividad.hasWindowFocus() }
    }

    @Test
    fun sesionTemporalAbreElCambioObligatorioDeContrasena() {
        esperarTexto(R.string.login_iniciar_sesion)
        falso.sesion.value = EstadoSesion.Temporal(sesion(Rol.ADMINISTRADOR))
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

        // Representa el estado emitido al vencer la sesión.
        // El vencimiento del reloj se comprueba en SesionStoreTest.
        compose.runOnIdle {
            falso.sesion.value = EstadoSesion.SinSesion
        }
        esperarTexto(R.string.login_iniciar_sesion)

        // Atrás no debe recuperar el perfil de la sesión anterior.
        val actividad = compose.activity
        pressBackUnconditionally()
        // Android 12 o superior puede enviar la actividad raíz al fondo.
        compose.waitUntil(5_000) { actividad.isFinishing || actividad.isDestroyed || !actividad.hasWindowFocus() }
    }

    @Test
    fun completarCambioObligatorioAbreElInicioDelRol() {
        esperarTexto(R.string.login_iniciar_sesion)
        val sesion = sesion(Rol.ADMINISTRADOR)

        compose.runOnIdle {
            falso.sesion.value = EstadoSesion.Temporal(sesion)
        }
        esperarTexto(R.string.aviso_cambio_password_obligatorio)

        // Representa el resultado de cambiar correctamente la contraseña.
        // Aquí se verifica la navegación, no el formulario ni el backend.
        compose.runOnIdle {
            falso.sesion.value = EstadoSesion.Autenticada(sesion)
        }
        esperarTexto(R.string.nav_solicitudes)
        esperarTexto(R.string.nav_conductores)
        esperarTexto(R.string.nav_vehiculos)

        // El cambio obligatorio no debe permanecer detrás del inicio.
        val actividad = compose.activity
        pressBackUnconditionally()
        // Android 12 o superior puede enviar la actividad raíz al fondo.
        compose.waitUntil(5_000) { actividad.isFinishing || actividad.isDestroyed || !actividad.hasWindowFocus() }
    }

    private fun entrarComo(rol: Rol) {
        runBlocking { sesiones.guardar(sesion(rol)) }
        falso.sesion.value = EstadoSesion.Autenticada(sesion(rol))
    }

    private fun salir() {
        falso.sesion.value = EstadoSesion.SinSesion
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
