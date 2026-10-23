package cl.belmontespa.myapplication.model

import cl.belmontespa.myapplication.ui.components.EtiquetaCalculada
import cl.belmontespa.myapplication.ui.components.etiquetaCalculada
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ventana de inicio, Atrasado y Vencido (README, Retraso y V3), con un `Clock` fijo. */
class RetrasoTest {

    private val zona = ZoneId.of("America/Santiago")
    private val fhAgenda = LocalDateTime.of(2026, 10, 28, 14, 30)

    /** Reloj fijo en `fhAgenda` más (o menos) esos minutos. */
    private fun relojA(minutos: Long, agenda: LocalDateTime = fhAgenda): Clock =
        Clock.fixed(agenda.plusMinutes(minutos).atZone(zona).toInstant(), zona)

    private fun viaje(
        estado: EstadoViaje = EstadoViaje.ASIGNADO,
        agenda: LocalDateTime = fhAgenda,
        pasajeros: Int = 0,
    ): Viaje = viajeDePrueba(fhAgenda = agenda, estado = estado, cantidadPasajeros = pasajeros)

    @Test
    fun unMinutoAntesDeLaVentanaNoPuedeIniciarse() {
        val v = viaje()
        assertFalse(v.enVentanaDeInicio(relojA(-61)))
        assertNull(v.retraso(relojA(-61)))
    }

    @Test
    fun laVentanaSeAbreUnaHoraAntesYIncluyeElExtremo() {
        val v = viaje()
        assertTrue(v.enVentanaDeInicio(relojA(-60)))
        assertTrue(v.enVentanaDeInicio(relojA(-59)))
        assertNull(v.retraso(relojA(-60)))
    }

    @Test
    fun laVentanaDeInicioDistingueAntesDentroYVencidoConAmbosBordesDentro() {
        fun en(minutos: Long) = ventanaDeInicio(fhAgenda, fhAgenda.plusMinutes(minutos))

        assertEquals(Inicio.ANTES_DE_LA_VENTANA, en(-61))
        assertEquals(Inicio.HABILITADO, en(-60))
        assertEquals(Inicio.HABILITADO, en(0))
        assertEquals(Inicio.HABILITADO, en(60))
        assertEquals(Inicio.VENCIDO, en(61))
        assertEquals(Inicio.ANTES_DE_LA_VENTANA, ventanaDeInicio(fhAgenda, fhAgenda.minusHours(1).minusSeconds(1)))
        assertEquals(Inicio.VENCIDO, ventanaDeInicio(fhAgenda, fhAgenda.plusHours(1).plusSeconds(1)))
    }

    @Test
    fun soloUnViajeAsignadoTieneVentanaDeInicio() {
        assertEquals(Inicio.HABILITADO, viaje().ventanaDeInicio(relojA(0)))
        for (estado in EstadoViaje.entries.filter { it != EstadoViaje.ASIGNADO }) {
            assertEquals(estado.name, Inicio.NO_APLICA, viaje(estado = estado).ventanaDeInicio(relojA(0)))
        }
    }

    @Test
    fun antesDeLaHoraAgendadaYEnPuntoNoHayRetraso() {
        val v = viaje()
        assertNull(v.retraso(relojA(-1)))
        assertNull(v.retraso(relojA(0)))
        assertTrue(v.enVentanaDeInicio(relojA(-1)))
        assertTrue(v.enVentanaDeInicio(relojA(0)))
    }

    @Test
    fun unMinutoDespuesDeLaHoraAgendadaEstaAtrasadoYAunPuedeIniciarse() {
        val v = viaje()
        assertEquals(Retraso.ATRASADO, v.retraso(relojA(1)))
        assertTrue(v.enVentanaDeInicio(relojA(1)))
    }

    @Test
    fun alCumplirseUnaHoraSigueAtrasadoYLaVentanaIncluyeElExtremo() {
        val v = viaje()
        assertEquals(Retraso.ATRASADO, v.retraso(relojA(60)))
        assertTrue(v.enVentanaDeInicio(relojA(60)))
    }

    @Test
    fun unMinutoDespuesDeLaHoraDeCierreEstaVencidoYFueraDeLaVentana() {
        val v = viaje()
        assertEquals(Retraso.VENCIDO, v.retraso(relojA(61)))
        assertFalse(v.enVentanaDeInicio(relojA(61)))
    }

    @Test
    fun unViajePendienteTambienPuedeLlevarRetraso() {
        val v = viaje(EstadoViaje.PENDIENTE)
        assertNull(v.retraso(relojA(0)))
        assertEquals(Retraso.ATRASADO, v.retraso(relojA(30)))
        assertEquals(Retraso.VENCIDO, v.retraso(relojA(90)))
    }

    @Test
    fun soloPendienteYAsignadoLlevanRetraso() {
        listOf(EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO, EstadoViaje.CANCELADO).forEach { estado ->
            val v = viaje(estado)
            assertNull(estado.name, v.retraso(relojA(30)))
            assertNull(estado.name, v.retraso(relojA(180)))
        }
    }

    @Test
    fun cruzaLaMedianocheConAritmeticaCivil() {
        val noche = LocalDateTime.of(2026, 10, 28, 23, 30)
        val v = viaje(agenda = noche)
        assertEquals(Retraso.ATRASADO, v.retraso(relojA(50, noche))) // 00:20 del día siguiente
        assertEquals(Retraso.ATRASADO, v.retraso(relojA(60, noche))) // 00:30
        assertEquals(Retraso.VENCIDO, v.retraso(relojA(61, noche))) // 00:31
        assertEquals(LocalDateTime.of(2026, 10, 28, 22, 30), v.disponibleDesde())
    }

    @Test
    fun disponibleDesdeEsUnaHoraAntesDeLaAgenda() {
        assertEquals(LocalDateTime.of(2026, 10, 28, 13, 30), viaje().disponibleDesde())
    }

    @Test
    fun seAsignaUnViajePendienteConPasajerosQueNoEstaVencido() {
        val v = viaje(EstadoViaje.PENDIENTE, pasajeros = 2)
        assertTrue(v.asignable(relojA(-600)))
        assertTrue(v.asignable(relojA(1))) // atrasado, pero aún puede iniciarse
        assertTrue(v.asignable(relojA(60))) // el extremo cuenta como no vencido
        assertFalse(v.asignable(relojA(61)))
    }

    @Test
    fun sinPasajerosNoSeAsigna() {
        assertFalse(viaje(EstadoViaje.PENDIENTE, pasajeros = 0).asignable(relojA(-600)))
    }

    @Test
    fun tambienSePuedeReasignarUnViajeAsignadoPeroNoLosDemasEstados() {
        assertTrue(viaje(EstadoViaje.ASIGNADO, pasajeros = 2).asignable(relojA(0)))
        listOf(EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO, EstadoViaje.CANCELADO).forEach { estado ->
            assertFalse(estado.name, viaje(estado, pasajeros = 2).asignable(relojA(0)))
        }
    }

    @Test
    fun cadaRetrasoTieneSuChip() {
        assertEquals(EtiquetaCalculada.ATRASADO, Retraso.ATRASADO.etiquetaCalculada)
        assertEquals(EtiquetaCalculada.VENCIDO, Retraso.VENCIDO.etiquetaCalculada)
    }
}
