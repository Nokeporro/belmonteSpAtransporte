package cl.belmontespa.myapplication.navegador

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavegadorAndroidTest {

    private val navegador = NavegadorAndroid(ApplicationProvider.getApplicationContext())
    private val punto = Ubicacion("Av. Apoquindo 5400", "Las Condes", "Metropolitana de Santiago", -33.41, -70.57)

    @Test
    fun googleMapsSeAbreConSuEsquemaYSuPaquete() {
        val intent = navegador.intent(AppNavegacion.GOOGLE_MAPS, punto)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("google.navigation:q=-33.410000,-70.570000", intent.data.toString())
        assertEquals("com.google.android.apps.maps", intent.`package`)
    }

    @Test
    fun wazeSeAbreConSuEnlaceYSuPaquete() {
        val intent = navegador.intent(AppNavegacion.WAZE, punto)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://waze.com/ul?ll=-33.410000,-70.570000&navigate=yes", intent.data.toString())
        assertEquals("com.waze", intent.`package`)
    }

    @Test
    fun elIntentSaleEnUnaTareaNuevaPorqueSeLanzaDesdeElContextoDeLaAplicacion() {
        val intent = navegador.intent(AppNavegacion.WAZE, punto)

        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun lasInstaladasSonUnSubconjuntoOrdenadoDeLasConocidas() {
        val instaladas = navegador.instaladas()

        assertEquals(AppNavegacion.entries.filter { it in instaladas }, instaladas)
    }

    @Test
    fun abrirUnaAplicacionAusenteDevuelveFalsoSinLanzar() {
        val ausente = AppNavegacion.entries.firstOrNull { it !in navegador.instaladas() } ?: return

        assertFalse(navegador.abrir(ausente, punto))
    }
}
