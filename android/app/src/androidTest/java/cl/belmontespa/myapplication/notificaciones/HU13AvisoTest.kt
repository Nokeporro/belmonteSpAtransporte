package cl.belmontespa.myapplication.notificaciones

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HU13AvisoTest {

    @get:Rule(order = 0)
    val entorno = EntornoPruebas()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val contexto: Context get() = ApplicationProvider.getApplicationContext()
    private var escenario: ActivityScenario<MainActivity>? = null

    @After
    fun cerrar() {
        escenario?.close()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Stage.entries.filter { it != Stage.DESTROYED }
                .flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
        contexto.getSystemService(NotificationManager::class.java).cancelAll()
    }

    @Test
    fun elAvisoDeAsignacionAbreElDetalleAlArrancarYAtrasVuelveALaAgenda() {
        entrar(Rol.CONDUCTOR)
        abrirCon(AvisoPush.desde("VIAJE", "TR-1042"))

        esperarTexto(titulo("TR-1042"))
        pressBack()

        esperarTexto(texto(R.string.nav_perfil))
        esperarSinTexto(titulo("TR-1042"))
    }

    @Test
    fun conLaAppAbiertaUnNuevoAvisoNavegaAlViajeYLaReasignacionVuelveALaAgenda() {
        entrar(Rol.CONDUCTOR)
        abrirCon(null)
        esperarTexto(texto(R.string.nav_perfil))

        enviar(AvisoPush.desde("VIAJE", "TR-1049"))
        esperarTexto(titulo("TR-1049"))

        enviar(AvisoPush.desde("AGENDA", "TR-1049"))
        esperarSinTexto(titulo("TR-1049"))
        esperarTexto(texto(R.string.nav_perfil))
    }

    @Test
    fun unAvisoQueNoCorrespondeAlRolNoCambiaDePantalla() {
        entrar(Rol.ADMINISTRADOR)
        abrirCon(AvisoPush.desde("AGENDA", "TR-1042"))

        esperarTexto(texto(R.string.nav_conductores))
        compose.waitForIdle()
        compose.onAllNodesWithText(titulo("TR-1042")).assertCountEquals(0)
    }

    @Test
    fun sinSesionElAvisoEsperaAlLoginYDespuesAbreElViaje() {
        abrirCon(AvisoPush.desde("VIAJE", "TR-1042"))
        esperarTexto(texto(R.string.login_iniciar_sesion))

        entrar(Rol.CONDUCTOR)

        esperarTexto(titulo("TR-1042"))
    }

    @Test
    fun conLaContrasenaTemporalElAvisoEsperaAlCambioObligatorio() {
        runBlocking { entorno.sesiones.guardarTemporal(sesion(Rol.CONDUCTOR)) }
        abrirCon(AvisoPush.desde("VIAJE", "TR-1042"))
        esperarTexto(texto(R.string.aviso_cambio_password_obligatorio))

        entrar(Rol.CONDUCTOR)

        esperarTexto(titulo("TR-1042"))
    }

    @Test
    fun elAvisoEnPrimerPlanoSePublicaConSuCanalYAlTocarloAbreElViaje() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        entrar(Rol.CONDUCTOR)

        PresentadorDeAvisos.presentar(
            contexto, AvisoPush.desde("VIAJE", "TR-1049", "Nuevo viaje asignado", "Viaje TR-1049 para 14-11-2026 · 23:00"),
        )

        val notificacion = esperarNotificacion("TR-1049").notification
        assertEquals(NotificacionesFirebase.CANAL_AVISOS, notificacion.channelId)
        assertEquals("Nuevo viaje asignado", notificacion.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Viaje TR-1049 para 14-11-2026 · 23:00", notificacion.extras.getCharSequence(Notification.EXTRA_TEXT).toString())

        notificacion.contentIntent.send()
        esperarTexto(titulo("TR-1049"))
    }

    private fun esperarNotificacion(etiqueta: String): StatusBarNotification {
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        val limite = System.currentTimeMillis() + 10_000
        var publicada: StatusBarNotification? = null
        while (publicada == null && System.currentTimeMillis() < limite) {
            publicada = administrador.activeNotifications.singleOrNull { it.tag == "VIAJE:$etiqueta" }
            if (publicada == null) Thread.sleep(200)
        }
        return checkNotNull(publicada) { "No quedó publicada la notificación de $etiqueta" }
    }

    private fun entrar(rol: Rol) {
        runBlocking { entorno.sesiones.guardar(sesion(rol)) }
    }

    private fun abrirCon(aviso: AvisoPush?) {
        escenario = ActivityScenario.launch(aviso?.aIntent(contexto) ?: Intent(contexto, MainActivity::class.java))
    }

    private fun enviar(aviso: AvisoPush) {
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        escenario!!.onActivity { actividad ->
            instrumentacion.callActivityOnNewIntent(actividad, aviso.aIntent(actividad))
        }
    }

    private fun sesion(rol: Rol) = Sesion(
        token = "prueba",
        correo = when (rol) {
            Rol.SOLICITANTE -> "crios@andesconsult.cl"
            Rol.ADMINISTRADOR -> "recepcion@belmontespa.cl"
            Rol.CONDUCTOR -> "ireyes@belmontespa.cl"
        },
        rol = rol,
        expiraEn = Long.MAX_VALUE,
    )

    private fun texto(@StringRes id: Int, vararg argumentos: Any) = contexto.getString(id, *argumentos)

    private fun titulo(folio: String) = texto(R.string.detalle_viaje_titulo, folio)

    private fun esperarTexto(buscado: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(buscado).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun esperarSinTexto(buscado: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(buscado).fetchSemanticsNodes().isEmpty() }
    }
}
