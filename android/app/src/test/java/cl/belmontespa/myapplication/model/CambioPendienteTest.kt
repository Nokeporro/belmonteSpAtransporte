package cl.belmontespa.myapplication.model

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CambioPendienteTest {

    private val agenda = LocalDateTime.of(2026, 10, 28, 12, 0)

    private fun operacion(id: String, destino: EstadoViaje, creadoEn: Long, envio: EstadoEnvio = EstadoEnvio.PENDIENTE, mensaje: String? = null) =
        OperacionPendienteEntity(id, "ireyes@ejemplo.cl", "TR-1042", destino.name, creadoEn, envio.name, mensaje)

    @Test
    fun unViajeSinCambioPendienteEsSuPropioEstadoVigente() {
        val viaje = viajeDePrueba(agenda, estado = EstadoViaje.ASIGNADO)

        assertSame(viaje, viaje.vigente())
    }

    @Test
    fun conUnCambioPendienteElEstadoVigenteEsElDestino() {
        val viaje = viajeDePrueba(agenda, estado = EstadoViaje.ASIGNADO, cambioPendiente = CambioPendiente(EstadoViaje.EN_CURSO))

        assertEquals(EstadoViaje.EN_CURSO, viaje.vigente().estado)
        assertEquals(EstadoViaje.ASIGNADO, viaje.estado)
    }

    @Test
    fun unCambioEnConflictoNoCambiaElEstadoVigente() {
        val viaje = viajeDePrueba(
            agenda, estado = EstadoViaje.ASIGNADO,
            cambioPendiente = CambioPendiente(EstadoViaje.EN_CURSO, enConflicto = true),
        )

        assertEquals(EstadoViaje.ASIGNADO, viaje.vigente().estado)
    }

    @Test
    fun sinOperacionesNoHayCambioPendiente() {
        assertNull(emptyList<OperacionPendienteEntity>().aCambioPendiente())
    }

    @Test
    fun conVariasOperacionesPendientesMandaLaUltima() {
        val cambio = listOf(
            operacion("a", EstadoViaje.EN_CURSO, creadoEn = 1),
            operacion("b", EstadoViaje.FINALIZADO, creadoEn = 2),
        ).aCambioPendiente()

        assertEquals(CambioPendiente(EstadoViaje.FINALIZADO), cambio)
    }

    @Test
    fun unaOperacionEnConflictoMandaAunqueHayaOtrasDespues() {
        val cambio = listOf(
            operacion("a", EstadoViaje.EN_CURSO, creadoEn = 1, envio = EstadoEnvio.CONFLICTO, mensaje = "El viaje está CANCELADO"),
            operacion("b", EstadoViaje.FINALIZADO, creadoEn = 2),
        ).aCambioPendiente()

        assertEquals(CambioPendiente(EstadoViaje.EN_CURSO, enConflicto = true, mensajeError = "El viaje está CANCELADO"), cambio)
    }
}
