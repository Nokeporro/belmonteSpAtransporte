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

    private fun viaje(estado: EstadoViaje = EstadoViaje.ASIGNADO, agenda: LocalDateTime = fhAgenda): Viaje {
        val lugar = Ubicacion("Av. Providencia 1234", "Providencia", "Metropolitana de Santiago", -33.4261, -70.611)
        return Viaje(
            folio = "TR-1042",
            estado = estado,
            correoSolicitante = "solicitante@ejemplo.cl",
            nombreSolicitante = "Solicitante",
            telefonoSolicitante = null,
            razonSocial = "Empresa",
            origen = lugar,
            destino = lugar,
            fhCreacion = agenda.minusDays(1),
            fhAgenda = agenda,
            fhAsignacion = null,
            fhInicio = null,
            fhFin = null,
            numeroVuelo = null,
            cantidadPasajeros = 0,
            correoConductor = null,
            nombreConductor = null,
            patenteMovil = null,
            hashPago = null,
            estadoPago = null,
            motivo = null,
            pasajeros = emptyList(),
        )
    }

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
    fun cadaRetrasoTieneSuChip() {
        assertEquals(EtiquetaCalculada.ATRASADO, Retraso.ATRASADO.etiquetaCalculada)
        assertEquals(EtiquetaCalculada.VENCIDO, Retraso.VENCIDO.etiquetaCalculada)
    }
}
