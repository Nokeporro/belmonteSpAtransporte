package cl.belmontespa.myapplication.viewmodel.solicitante

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.model.viajeDePrueba
import cl.belmontespa.myapplication.viewmodel.ReposP1Falsos
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleSolicitudViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneId.of("America/Santiago"))
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: DetalleSolicitudViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.viajeActual.value = viajeDePrueba(LocalDateTime.now(clock).plusHours(3))
            .copy(hashPago = "pago", estadoPago = EstadoPago.PAGADO)
        repos.pago = Pago("pago", repos.viajeActual.value!!.folio, 50001, EstadoPago.PAGADO, "Banco", 12,
            "12345678-5", MedioPago.TRANSFERENCIA, TipoRecibo.BOLETA, LocalDateTime.now(clock), null)
        vm = DetalleSolicitudViewModel(aplicacionFalsa(), repos.viajes, repos.pagos, clock)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun cargar() = vm.cargar(repos.viajeActual.value!!.folio)

    @Test fun muestraLosCincoEstadosYElReembolsoDeLaSolicitud() = runTest(dispatcher) {
        cargar(); runCurrent()
        for (estado in EstadoPago.entries) {
            repos.pago = repos.pago!!.copy(estado = estado, montoReembolsado = if (estado == EstadoPago.REEMBOLSADO) 25000 else null)
            repos.viajeActual.value = repos.viajeActual.value!!.copy(estadoPago = estado); runCurrent()
            assertEquals(repos.pago, vm.estado.value.pago); assertNull(vm.estado.value.errorPago)
            assertFalse(vm.estado.value.cargandoPago)
        }
    }

    @Test fun sinPagoNoConsultaNiConservaElPagoAnterior() = runTest(dispatcher) {
        cargar(); runCurrent(); assertNotNull(vm.estado.value.pago)
        repos.viajeActual.value = repos.viajeActual.value!!.copy(hashPago = null, estadoPago = null); runCurrent()
        assertEquals(1, repos.consultasPago); assertNull(vm.estado.value.pago); assertNull(vm.estado.value.errorPago)
    }

    @Test fun elFalloDelPagoConservaLaSolicitudYLaObservacionYPermiteReintentar() = runTest(dispatcher) {
        repos.errorPago = ErrorApp.SinConexion; cargar(); runCurrent()
        assertNotNull(vm.estado.value.viaje); assertNull(vm.estado.value.error)
        assertEquals(ErrorApp.SinConexion, vm.estado.value.errorPago); assertNull(vm.estado.value.pago)
        repos.errorPago = null; vm.reintentar(); runCurrent()
        assertEquals(repos.pago, vm.estado.value.pago); assertNull(vm.estado.value.errorPago)
        repos.pago = repos.pago!!.copy(estado = EstadoPago.ANULADO)
        repos.viajeActual.value = repos.viajeActual.value!!.copy(estado = EstadoViaje.CANCELADO, estadoPago = EstadoPago.ANULADO); runCurrent()
        assertEquals(EstadoPago.ANULADO, vm.estado.value.pago!!.estado)
    }

    @Test fun unaConsultaLentaNoOcultaElViajeNiReapareceSiElViajeYaNoTienePago() = runTest(dispatcher) {
        repos.esperaPago = CompletableDeferred(); cargar(); runCurrent()
        assertNotNull(vm.estado.value.viaje); assertFalse(vm.estado.value.cargando); assertTrue(vm.estado.value.cargandoPago)
        repos.viajeActual.value = repos.viajeActual.value!!.copy(hashPago = null, estadoPago = null); runCurrent()
        repos.esperaPago!!.complete(Unit); runCurrent()
        assertNull(vm.estado.value.pago); assertFalse(vm.estado.value.cargandoPago)
    }

    @Test fun alVolverAPrimerPlanoRecargaElPagoAunqueElViajeNoHayaEmitido() = runTest(dispatcher) {
        cargar(); runCurrent(); repos.pago = repos.pago!!.copy(estado = EstadoPago.REEMBOLSADO, montoReembolsado = 25000)
        vm.alVolverAPrimerPlano(); runCurrent()
        assertEquals(2, repos.consultasPago); assertEquals(25000, vm.estado.value.pago!!.montoReembolsado)
    }

    @Test fun unaSolicitudInexistenteSeInformaComoNoEncontrada() = runTest(dispatcher) {
        repos.viajeActual.value = null; vm.cargar("TR-9999"); runCurrent()
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error); assertNull(vm.estado.value.viaje)
    }
}
