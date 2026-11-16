package cl.belmontespa.myapplication.notificaciones

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HU12AvisosTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val contexto: Context get() = ApplicationProvider.getApplicationContext()
    private val manager get() = contexto.getSystemService(NotificationManager::class.java)
    private fun aviso(folio: String = "TR-1042") = AvisoPush.deDatos(mapOf("tipo" to "VIAJE", "folio" to folio))!!
    private fun sesion(rol: Rol = Rol.SOLICITANTE) = Sesion("prueba",
        if (rol == Rol.ADMINISTRADOR) "recepcion@belmontespa.cl" else "crios@andesconsult.cl", rol, Long.MAX_VALUE)

    @Before fun preparar() {
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
        manager.cancelAll()
        esperar(R.string.login_iniciar_sesion)
    }
    @After fun limpiar() { manager.cancelAll() }

    private fun esperar(@StringRes id: Int) = esperarTexto(contexto.getString(id))
    private fun esperarTexto(texto: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun abrir(aviso: AvisoPush) {
        contexto.startActivity(AvisosAndroid.intent(contexto, aviso).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun entrar() {
        runBlocking { entorno.sesiones.guardar(sesion()) }
        esperar(R.string.nav_mis_solicitudes)
    }
    private fun detalle(folio: String = "TR-1042") {
        esperar(R.string.t06_detalle_solicitud)
        esperarTexto(folio)
    }

    @Test fun elAvisoAbreElDetalleYVolverLlevaAMisSolicitudes() {
        entrar(); abrir(aviso()); detalle()
        pressBack(); esperar(R.string.nav_mis_solicitudes)
        // Un aviso ya consumido no reaparece al recrear la actividad en otra pantalla.
        compose.onNodeWithText(contexto.getString(R.string.nav_perfil)).performClick()
        esperar(R.string.accion_cerrar_sesion)
        compose.activityRule.scenario.recreate()
        esperar(R.string.accion_cerrar_sesion)
    }

    @Test fun esperaElLoginYConservaElAvisoDuranteLaRecreacion() {
        abrir(aviso()); compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        esperar(R.string.login_iniciar_sesion)
        runBlocking { entorno.sesiones.guardar(sesion()) }
        detalle()
    }

    @Test fun unaSesionTemporalDebeCambiarSuClaveAntesDeAbrirElViaje() {
        runBlocking { entorno.sesiones.guardarTemporal(sesion()) }
        esperar(R.string.aviso_cambio_password_obligatorio)
        abrir(aviso()); compose.waitForIdle()
        esperar(R.string.aviso_cambio_password_obligatorio)
        runBlocking { entorno.sesiones.guardar(sesion()) }
        detalle()
    }

    @Test fun noAbreCuentasConUnRolDeSolicitante() {
        entrar()
        abrir(AvisoPush.deDatos(mapOf("tipo" to "CUENTAS"))!!)
        compose.waitForIdle()
        esperar(R.string.nav_mis_solicitudes)
        assertTrue(compose.onAllNodesWithText(contexto.getString(R.string.t06_cuentas_pendientes))
            .fetchSemanticsNodes().isEmpty())
    }

    @Test fun dosNotificacionesConservanSuViajeYPermitenAbrirAvisosConLaAppActiva() {
        entrar()
        assertTrue(AvisosAndroid.mostrar(contexto, aviso(), "Viaje TR-1042", "Estado: En curso"))
        assertTrue(AvisosAndroid.mostrar(contexto, aviso("TR-1039"), "Viaje TR-1039", "Estado: Finalizado"))
        compose.waitUntil(5_000) { manager.activeNotifications.size == 2 }
        val publicadas = manager.activeNotifications
        assertEquals(2, publicadas.size)
        val primera = publicadas.single { it.tag == "VIAJE:TR-1042" }.notification
        assertEquals("avisos", primera.channelId)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel("avisos").importance)
        assertEquals("Estado: En curso", primera.extras.getString(Notification.EXTRA_TEXT))
        assertTrue(primera.flags and Notification.FLAG_AUTO_CANCEL != 0)
        primera.contentIntent.send(); detalle()
        val segunda = publicadas.single { it.tag == "VIAJE:TR-1039" }.notification
        assertNotEquals(primera.contentIntent, segunda.contentIntent)
        segunda.contentIntent.send(); detalle("TR-1039")
    }

    @Test fun unPayloadDeSegundoPlanoUsaElRolActualYRechazaUnaRutaInventada() {
        runBlocking { entorno.sesiones.guardar(sesion(Rol.ADMINISTRADOR)) }
        esperar(R.string.nav_solicitudes)
        contexto.startActivity(Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("tipo", "VIAJE"); putExtra("folio", "TR-1042")
            putExtra("ruta", "cambiar_password"); putExtra("rol", "SOLICITANTE")
        })
        esperar(R.string.t06_detalle_admin)
        esperarTexto("TR-1042")
    }
}
