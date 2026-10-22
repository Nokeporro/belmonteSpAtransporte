package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** V2 (README, Viaje): cada regla de asignar o reasignar, con su texto y su orden. */
class ReglasAsignacionTest {

    private val agenda = LocalDateTime.of(2026, 10, 28, 12, 0)
    private val ahora = agenda.minusHours(5)

    private fun participante(
        activo: Boolean = true,
        disponible: Boolean = true,
        otros: List<LocalDateTime> = emptyList(),
        yaAsignado: Boolean = false,
    ) = ParticipanteAsignacion(activo, disponible, otros, yaAsignado)

    private fun conflicto(
        estado: EstadoViaje = EstadoViaje.PENDIENTE,
        pasajeros: Int = 2,
        ahora: LocalDateTime = this.ahora,
        conductor: ParticipanteAsignacion = participante(),
        movil: ParticipanteAsignacion = participante(),
        capacidad: Int = 4,
        habilitado: Boolean = true,
    ) = conflictoDeAsignacion(estado, agenda, pasajeros, ahora, conductor, movil, capacidad, habilitado)

    @Test
    fun unaAsignacionCorrectaNoTieneConflicto() {
        assertNull(conflicto())
    }

    @Test
    fun soloSeAsignaUnViajePendienteOAsignado() {
        assertNull(conflicto(estado = EstadoViaje.ASIGNADO))
        listOf(EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO, EstadoViaje.CANCELADO).forEach {
            assertEquals("El viaje ya no se puede asignar", conflicto(estado = it))
        }
    }

    @Test
    fun elViajeSeVenceUnaHoraDespuesDeLaAgenda() {
        assertNull(conflicto(ahora = agenda.plusHours(1)))
        assertEquals("El viaje está vencido", conflicto(ahora = agenda.plusHours(1).plusSeconds(1)))
    }

    @Test
    fun sinPasajerosNoSeAsigna() {
        assertEquals("El viaje no tiene pasajeros", conflicto(pasajeros = 0))
    }

    @Test
    fun elVencimientoSeRevisaAntesQueLosPasajeros() {
        assertEquals("El viaje está vencido", conflicto(pasajeros = 0, ahora = agenda.plusHours(2)))
    }

    @Test
    fun elConductorTieneQueEstarActivoYDisponible() {
        assertEquals("El conductor no está activo", conflicto(conductor = participante(activo = false)))
        assertEquals("El conductor no está disponible", conflicto(conductor = participante(disponible = false)))
    }

    @Test
    fun elVehiculoTieneQueEstarActivoYDisponible() {
        assertEquals("El móvil no está activo", conflicto(movil = participante(activo = false)))
        assertEquals("El móvil no está disponible", conflicto(movil = participante(disponible = false)))
    }

    @Test
    fun elParTieneQueEstarHabilitado() {
        assertEquals("El conductor no está habilitado para el móvil", conflicto(habilitado = false))
    }

    @Test
    fun laCapacidadTieneQueCubrirALosPasajeros() {
        assertNull(conflicto(pasajeros = 4, capacidad = 4))
        assertEquals("Los pasajeros superan la capacidad del móvil", conflicto(pasajeros = 5, capacidad = 4))
    }

    @Test
    fun otroViajeDelConductorAMenosDeDosHorasChoca() {
        listOf(-119L, -1L, 0L, 1L, 119L).forEach { minutos ->
            val conductor = participante(otros = listOf(agenda.plusMinutes(minutos)))
            assertEquals("a $minutos min", "El conductor ya tiene un viaje a menos de 2 horas", conflicto(conductor = conductor))
        }
    }

    @Test
    fun aDosHorasExactasNoChoca() {
        assertNull(conflicto(conductor = participante(otros = listOf(agenda.minusMinutes(120), agenda.plusMinutes(120)))))
        assertNull(conflicto(movil = participante(otros = listOf(agenda.minusMinutes(120), agenda.plusMinutes(120)))))
    }

    @Test
    fun otroViajeDelVehiculoAMenosDeDosHorasChoca() {
        val movil = participante(otros = listOf(agenda.plusMinutes(90)))
        assertEquals("El móvil ya tiene un viaje a menos de 2 horas", conflicto(movil = movil))
    }

    @Test
    fun alReasignarSoloSeValidaLoQueCambia() {
        // Mismo conductor, Ocupado e incluso con otro viaje cerca: no cambia, no se revisa.
        val mismoConductor = participante(disponible = false, otros = listOf(agenda.plusMinutes(30)), yaAsignado = true)
        assertNull(conflicto(conductor = mismoConductor))
        // Mismo vehículo, En uso: tampoco.
        val mismoMovil = participante(disponible = false, activo = false, otros = listOf(agenda), yaAsignado = true)
        assertNull(conflicto(movil = mismoMovil))
    }

    @Test
    fun alCambiarSoloElVehiculoSigueValidandoseElVehiculo() {
        val mismoConductor = participante(disponible = false, yaAsignado = true)
        assertEquals("El móvil no está disponible", conflicto(conductor = mismoConductor, movil = participante(disponible = false)))
        assertEquals(
            "El móvil ya tiene un viaje a menos de 2 horas",
            conflicto(conductor = mismoConductor, movil = participante(otros = listOf(agenda.plusMinutes(60)))),
        )
        // Y el conductor que no cambia no choca con sus propios viajes.
        assertNull(conflicto(conductor = participante(otros = listOf(agenda), yaAsignado = true)))
    }

    @Test
    fun alCambiarSoloElConductorSigueValidandoseElConductor() {
        val mismoMovil = participante(disponible = false, yaAsignado = true)
        assertEquals("El conductor no está disponible", conflicto(conductor = participante(disponible = false), movil = mismoMovil))
        assertEquals(
            "El conductor ya tiene un viaje a menos de 2 horas",
            conflicto(conductor = participante(otros = listOf(agenda.minusMinutes(30))), movil = mismoMovil),
        )
    }

    @Test
    fun repetirLaMismaAsignacionNoValidaNadaMasQueElViaje() {
        val igual = participante(activo = false, disponible = false, yaAsignado = true)
        assertNull(conflicto(conductor = igual, movil = igual, habilitado = false))
        assertEquals("El viaje no tiene pasajeros", conflicto(conductor = igual, movil = igual, pasajeros = 0))
    }
}
