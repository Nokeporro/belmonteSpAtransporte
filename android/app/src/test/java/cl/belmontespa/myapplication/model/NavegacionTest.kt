package cl.belmontespa.myapplication.model

import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavegacionTest {

    private val origen = Ubicacion("Aeropuerto A. Merino Benítez", "Pudahuel", "Metropolitana de Santiago", -33.393, -70.786)
    private val destino = Ubicacion("Av. Apoquindo 5400", "Las Condes", "Metropolitana de Santiago", -33.41, -70.57)

    private fun viaje(estado: EstadoViaje): Viaje =
        viajeDePrueba(fhAgenda = LocalDateTime.of(2026, 10, 28, 14, 30), estado = estado).copy(origen = origen, destino = destino)

    @Test
    fun unViajeAsignadoNavegaAlOrigen() {
        assertEquals(origen, viaje(EstadoViaje.ASIGNADO).puntoDeNavegacion())
    }

    @Test
    fun unViajeEnCursoNavegaAlDestino() {
        assertEquals(destino, viaje(EstadoViaje.EN_CURSO).puntoDeNavegacion())
    }

    @Test
    fun enCualquierOtroEstadoNoHayRuta() {
        for (estado in EstadoViaje.entries.filter { it != EstadoViaje.ASIGNADO && it != EstadoViaje.EN_CURSO }) {
            assertNull(estado.name, viaje(estado).puntoDeNavegacion())
        }
    }

    @Test
    fun googleMapsRecibeLasCoordenadasEnSuEsquemaDeNavegacion() {
        assertEquals("google.navigation:q=-33.393000,-70.786000", AppNavegacion.GOOGLE_MAPS.uri(origen))
    }

    @Test
    fun wazeRecibeLasCoordenadasEnSuEnlaceYPideNavegar() {
        assertEquals("https://waze.com/ul?ll=-33.410000,-70.570000&navigate=yes", AppNavegacion.WAZE.uri(destino))
    }

    @Test
    fun cadaAplicacionTieneSuPaqueteYElOrdenEsElDeLaHoja() {
        assertEquals(listOf(AppNavegacion.GOOGLE_MAPS, AppNavegacion.WAZE), AppNavegacion.entries)
        assertEquals("com.google.android.apps.maps", AppNavegacion.GOOGLE_MAPS.paquete)
        assertEquals("com.waze", AppNavegacion.WAZE.paquete)
        assertEquals("Google Maps", AppNavegacion.GOOGLE_MAPS.nombre)
        assertEquals("Waze", AppNavegacion.WAZE.nombre)
    }

    @Test
    fun lasCoordenadasSeDanConSeisDecimalesSinNotacionCientifica() {
        val cerca = Ubicacion("Punto", "Comuna", "Región", 0.00001, -0.5)
        assertEquals("google.navigation:q=0.000010,-0.500000", AppNavegacion.GOOGLE_MAPS.uri(cerca))
    }

    @Test
    fun elIdiomaDelTelefonoNoCambiaElPuntoDecimal() {
        val anterior = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("google.navigation:q=-33.393000,-70.786000", AppNavegacion.GOOGLE_MAPS.uri(origen))
            assertEquals("https://waze.com/ul?ll=-33.393000,-70.786000&navigate=yes", AppNavegacion.WAZE.uri(origen))
        } finally {
            Locale.setDefault(anterior)
        }
    }
}
