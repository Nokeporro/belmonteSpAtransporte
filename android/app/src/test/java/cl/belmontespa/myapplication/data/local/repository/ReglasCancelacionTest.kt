package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.entity.PagoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** P4 y P5 al cancelar un viaje (README, Pago): el 50 % del Solicitante y el reembolso del Administrador. */
class ReglasCancelacionTest {

    private fun pago(estado: String, monto: Int = 52_000, reembolsado: Int? = null) = PagoEntity(
        "pago-1", "TR-1038", monto, estado, "Banco de prueba", 12, "76543210-3", "TRANSFERENCIA", "FACTURA",
        "2026-10-20T12:00:00", reembolsado,
    )

    @Test
    fun elSolicitanteRecibeLaMitadRedondeadaHaciaAbajoAunqueResulteCero() {
        for (monto in listOf(50_001, 52_000, 1)) {
            val resultado = pago("PAGADO", monto).alCancelarElViaje(administrador = false, vencido = false, fijado = null)

            assertEquals(pago("REEMBOLSADO", monto, monto / 2), resultado)
        }
    }

    @Test
    fun elAdministradorReembolsaTodoSiElViajeNoEstaVencidoEIgnoraElMontoEnviado() {
        for (fijado in listOf(null, 0, 1, 999_999)) {
            val resultado = pago("PAGADO").alCancelarElViaje(administrador = true, vencido = false, fijado = fijado)

            assertEquals(pago("REEMBOLSADO", 52_000, 52_000), resultado)
        }
    }

    @Test
    fun conElViajeVencidoElAdministradorFijaElMonto() {
        assertEquals(pago("REEMBOLSADO", reembolsado = 20_000), pago("PAGADO").alCancelarElViaje(true, true, 20_000))
        assertEquals(pago("REEMBOLSADO", reembolsado = 52_000), pago("PAGADO").alCancelarElViaje(true, true, 52_000))
    }

    @Test
    fun unReembolsoDeCeroDelAdministradorDejaElPagoPagado() {
        assertEquals(pago("PAGADO"), pago("PAGADO").alCancelarElViaje(true, true, 0))
    }

    @Test
    fun unPagoPendienteQuedaAnuladoSinReembolsoParaLosDosRoles() {
        for (administrador in listOf(false, true)) {
            for (vencido in listOf(false, true)) {
                assertEquals(pago("ANULADO"), pago("PENDIENTE").alCancelarElViaje(administrador, vencido, null))
            }
        }
    }

    @Test
    fun losPagosTerminalesNoCambian() {
        for (estado in listOf("RECHAZADO", "ANULADO", "REEMBOLSADO")) {
            val terminal = pago(estado, reembolsado = if (estado == "REEMBOLSADO") 7_000 else null)

            assertEquals(terminal, terminal.alCancelarElViaje(administrador = false, vencido = false, fijado = null))
            assertEquals(terminal, terminal.alCancelarElViaje(administrador = true, vencido = true, fijado = 1))
        }
    }

    @Test
    fun conElViajeVencidoYSinMontoValidadoLaReglaSeNiega() {
        assertThrows(IllegalArgumentException::class.java) { pago("PAGADO").alCancelarElViaje(true, true, null) }
    }
}
