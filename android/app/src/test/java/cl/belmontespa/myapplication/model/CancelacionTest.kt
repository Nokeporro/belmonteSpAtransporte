package cl.belmontespa.myapplication.model

import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class CancelacionTest {
    private val agenda = LocalDateTime.of(2026, 10, 23, 14, 30)
    @Test fun pendientesYAsignadosSonCancelablesHastaUnaHoraInclusive() {
        for (estado in listOf(EstadoViaje.PENDIENTE, EstadoViaje.ASIGNADO)) {
            val viaje = viajeDePrueba(agenda, estado = estado)
            assertTrue(viaje.cancelablePorSolicitante(agenda.minusDays(1)))
            assertTrue(viaje.cancelablePorSolicitante(agenda.plusHours(1)))
            assertFalse(viaje.cancelablePorSolicitante(agenda.plusHours(1).plusNanos(1)))
        }
    }
    @Test fun noSeCancelanViajesEnCursoFinalizadosOCancelados() {
        for (estado in listOf(EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO, EstadoViaje.CANCELADO)) {
            assertFalse(viajeDePrueba(agenda, estado = estado).cancelablePorSolicitante(agenda))
        }
    }
}
