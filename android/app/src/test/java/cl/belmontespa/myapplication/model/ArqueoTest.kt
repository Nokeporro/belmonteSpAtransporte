package cl.belmontespa.myapplication.model

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Arqueo de abordaje (README, RF29): «Abordaron X de N» y quiénes faltan. */
class ArqueoTest {

    private val agenda = LocalDateTime.of(2026, 10, 28, 14, 30)
    private val abordo = agenda.minusMinutes(5)

    private fun viaje(cantidad: Int, vararg pasajeros: PasajeroViaje, estado: EstadoViaje = EstadoViaje.ASIGNADO) =
        viajeDePrueba(agenda, estado = estado, cantidadPasajeros = cantidad, pasajeros = pasajeros.toList())

    @Test
    fun sinPasajerosNoHayArqueo() {
        assertNull(viaje(0).arqueo())
    }

    @Test
    fun conNingunoABordoFaltanTodos() {
        val arqueo = viaje(2, pasajeroDePrueba("Constanza Ríos"), pasajeroDePrueba("Tomás Vidal")).arqueo()!!

        assertEquals(0, arqueo.abordaron)
        assertEquals(2, arqueo.total)
        assertEquals(listOf("Constanza Ríos", "Tomás Vidal"), arqueo.faltan)
        assertFalse(arqueo.completo)
    }

    @Test
    fun conAlgunosABordoFaltanLosDemas() {
        val arqueo = viaje(2, pasajeroDePrueba("Constanza Ríos", abordo), pasajeroDePrueba("Tomás Vidal")).arqueo()!!

        assertEquals(1, arqueo.abordaron)
        assertEquals(listOf("Tomás Vidal"), arqueo.faltan)
        assertFalse(arqueo.completo)
    }

    @Test
    fun conTodosABordoEstaCompleto() {
        val arqueo = viaje(2, pasajeroDePrueba("Constanza Ríos", abordo), pasajeroDePrueba("Tomás Vidal", abordo)).arqueo()!!

        assertEquals(2, arqueo.abordaron)
        assertTrue(arqueo.faltan.isEmpty())
        assertTrue(arqueo.completo)
    }

    @Test
    fun losFaltantesConservanElOrdenDeLosPasajeros() {
        val arqueo = viaje(
            3,
            pasajeroDePrueba("Ana"), pasajeroDePrueba("Beto", abordo), pasajeroDePrueba("Carla"),
        ).arqueo()!!

        assertEquals(listOf("Ana", "Carla"), arqueo.faltan)
    }

    @Test
    fun hayArqueoCuandoElViajeEstaAsignadoEnCursoOFinalizado() {
        for (estado in listOf(EstadoViaje.ASIGNADO, EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO)) {
            val arqueo = viaje(2, pasajeroDePrueba("Ana", abordo), pasajeroDePrueba("Beto"), estado = estado).arqueo()

            assertEquals(estado.name, 1, arqueo?.abordaron)
            assertEquals(estado.name, listOf("Beto"), arqueo?.faltan)
        }
    }

    @Test
    fun enCualquierOtroEstadoNoHayArqueoAunqueAlguienHayaAbordado() {
        for (estado in EstadoViaje.entries.filter { it !in listOf(EstadoViaje.ASIGNADO, EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO) }) {
            assertNull(estado.name, viaje(2, pasajeroDePrueba("Ana", abordo), pasajeroDePrueba("Beto"), estado = estado).arqueo())
        }
    }

    @Test
    fun elTotalEsLaCantidadDePasajerosDelViaje() {
        // N es `cantidad_pasajeros`, aunque la lista traiga menos filas.
        val arqueo = viaje(3, pasajeroDePrueba("Ana", abordo)).arqueo()!!

        assertEquals(1, arqueo.abordaron)
        assertEquals(3, arqueo.total)
        assertFalse(arqueo.completo)
    }
}
