package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasAbordajeTest {

    private val agenda = LocalDateTime.of(2026, 10, 28, 14, 30)

    @Test
    fun unViajeAsignadoEsCandidatoDentroDeLaVentanaConAmbosExtremosIncluidos() {
        assertTrue(esCandidatoDeAbordaje(EstadoViaje.ASIGNADO, agenda, agenda))
        assertTrue(esCandidatoDeAbordaje(EstadoViaje.ASIGNADO, agenda, agenda.minusHours(1)))
        assertTrue(esCandidatoDeAbordaje(EstadoViaje.ASIGNADO, agenda, agenda.plusHours(1)))
    }

    @Test
    fun unViajeAsignadoFueraDeLaVentanaNoEsCandidato() {
        assertFalse(esCandidatoDeAbordaje(EstadoViaje.ASIGNADO, agenda, agenda.minusHours(1).minusSeconds(1)))
        assertFalse(esCandidatoDeAbordaje(EstadoViaje.ASIGNADO, agenda, agenda.plusHours(1).plusSeconds(1)))
    }

    @Test
    fun unViajeEnCursoEsCandidatoAunqueLaAgendaEsteLejos() {
        assertTrue(esCandidatoDeAbordaje(EstadoViaje.EN_CURSO, agenda, agenda.plusDays(2)))
        assertTrue(esCandidatoDeAbordaje(EstadoViaje.EN_CURSO, agenda, agenda.minusDays(2)))
    }

    @Test
    fun enCualquierOtroEstadoNuncaEsCandidato() {
        for (estado in EstadoViaje.entries.filter { it != EstadoViaje.ASIGNADO && it != EstadoViaje.EN_CURSO }) {
            assertFalse(estado.name, esCandidatoDeAbordaje(estado, agenda, agenda))
        }
    }

    @Test
    fun siElDocumentoCalzaEnVariosViajesGanaElDeAgendaMasCercanaAAhora() {
        val ahora = LocalDateTime.of(2026, 10, 28, 12, 0)
        val lejano = ahora.plusMinutes(50)
        val cercano = ahora.plusMinutes(10)
        val pasado = ahora.minusMinutes(30)

        assertEquals(listOf(cercano, pasado, lejano), ordenarPorCercania(listOf(lejano, cercano, pasado), ahora) { it })
    }

    @Test
    fun aIgualDistanciaConservaElOrdenDeEntrada() {
        val ahora = LocalDateTime.of(2026, 10, 28, 12, 0)
        val antes = ahora.minusMinutes(20)
        val despues = ahora.plusMinutes(20)

        assertEquals(listOf(despues, antes), ordenarPorCercania(listOf(despues, antes), ahora) { it })
        assertEquals(listOf(antes, despues), ordenarPorCercania(listOf(antes, despues), ahora) { it })
    }
}
