package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** V3 y V4 (README, Viaje): los mismos mensajes y el mismo orden que `ViajeService.cambiarEstado`. */
class ReglasEstadoTest {

    private val agenda = LocalDateTime.of(2026, 10, 28, 14, 30)

    private fun conflicto(
        actual: EstadoViaje = EstadoViaje.ASIGNADO,
        nuevo: EstadoViaje = EstadoViaje.EN_CURSO,
        ahora: LocalDateTime = agenda,
        conductorOcupado: Boolean = false,
        movilEnUso: Boolean = false,
    ) = conflictoDeEstado(actual, nuevo, agenda, ahora, conductorOcupado, movilEnUso)

    @Test
    fun iniciarUnViajeAsignadoDentroDeLaVentanaSePermite() {
        assertNull(conflicto())
    }

    @Test
    fun lasTransicionesInvalidasDicenDeQueEstadoANingunLado() {
        assertEquals("No se puede pasar de ASIGNADO a FINALIZADO", conflicto(nuevo = EstadoViaje.FINALIZADO))
        assertEquals("No se puede pasar de EN_CURSO a EN_CURSO", conflicto(actual = EstadoViaje.EN_CURSO))
        assertEquals("No se puede pasar de FINALIZADO a EN_CURSO", conflicto(actual = EstadoViaje.FINALIZADO))
        assertEquals("No se puede pasar de PENDIENTE a EN_CURSO", conflicto(actual = EstadoViaje.PENDIENTE))
        assertEquals("No se puede pasar de CANCELADO a EN_CURSO", conflicto(actual = EstadoViaje.CANCELADO))
        assertEquals("No se puede pasar de ASIGNADO a CANCELADO", conflicto(nuevo = EstadoViaje.CANCELADO))
    }

    @Test
    fun laVentanaIncluyeAmbosBordes() {
        assertNull(conflicto(ahora = agenda.minusHours(1)))
        assertNull(conflicto(ahora = agenda.plusHours(1)))
    }

    @Test
    fun unSegundoFueraDeLaVentanaNoSePuedeIniciar() {
        val mensaje = "El viaje está fuera de la ventana de inicio"
        assertEquals(mensaje, conflicto(ahora = agenda.minusHours(1).minusSeconds(1)))
        assertEquals(mensaje, conflicto(ahora = agenda.plusHours(1).plusSeconds(1)))
    }

    @Test
    fun elConductorConOtroViajeEnCursoNoPuedeIniciar() {
        assertEquals("El conductor ya tiene un viaje en curso", conflicto(conductorOcupado = true))
    }

    @Test
    fun elMovilConOtroViajeEnCursoNoPuedeIniciar() {
        assertEquals("El móvil ya tiene un viaje en curso", conflicto(movilEnUso = true))
    }

    @Test
    fun elOrdenEsTransicionVentanaConductorYMovil() {
        val fuera = agenda.plusHours(3)
        assertEquals(
            "No se puede pasar de PENDIENTE a EN_CURSO",
            conflicto(actual = EstadoViaje.PENDIENTE, ahora = fuera, conductorOcupado = true, movilEnUso = true),
        )
        assertEquals(
            "El viaje está fuera de la ventana de inicio",
            conflicto(ahora = fuera, conductorOcupado = true, movilEnUso = true),
        )
        assertEquals("El conductor ya tiene un viaje en curso", conflicto(conductorOcupado = true, movilEnUso = true))
    }

    @Test
    fun finalizarNoMiraLaVentanaNiLosOtrosViajes() {
        val dosDiasDespues = agenda.plusDays(2)
        assertNull(
            conflicto(
                actual = EstadoViaje.EN_CURSO, nuevo = EstadoViaje.FINALIZADO, ahora = dosDiasDespues,
                conductorOcupado = true, movilEnUso = true,
            ),
        )
    }
}
