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
class ConfirmarCancelacionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"), ZoneId.of("America/Santiago"))
    private val ahora = LocalDateTime.now(clock)
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: ConfirmarCancelacionViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.viajeActual.value = viajeDePrueba(ahora.plusHours(3))
        vm = ConfirmarCancelacionViewModel(aplicacionFalsa(), repos.viajes, repos.pagos, clock)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun conPago(estado: EstadoPago = EstadoPago.PAGADO) {
        repos.pago = Pago("pago-prueba", "TR-1051", 50_001, estado, "Banco", 12, "12345678-5", MedioPago.TRANSFERENCIA, TipoRecibo.BOLETA, ahora.minusDays(1), null)
        repos.viajeActual.value = repos.viajeActual.value!!.copy(hashPago = "pago-prueba", estadoPago = estado)
    }

    @Test fun cargaElPagoAntesDeConfirmar() = runTest(dispatcher) {
        conPago(); vm.cargar("TR-1051"); runCurrent()
        assertEquals(50_001, vm.estado.value.pago?.monto)
        assertEquals(EstadoPago.PAGADO, vm.estado.value.pago?.estado)
        assertNull(vm.estado.value.error); assertFalse(vm.estado.value.cargando)
    }

    @Test fun dobleToqueCancelaUnaVezYSoloEnviaElFolio() = runTest(dispatcher) {
        conPago(); vm.cargar("TR-1051"); runCurrent()
        repos.espera = CompletableDeferred()
        vm.confirmar(); vm.confirmar(); runCurrent()
        assertEquals(1, repos.cancelaciones); assertTrue(vm.estado.value.enviando)
        assertNull(repos.motivoCancelacion); assertNull(repos.montoCancelacion)
        repos.espera!!.complete(Unit); runCurrent()
        assertTrue(vm.estado.value.cancelada); assertFalse(vm.estado.value.enviando)
    }

    @Test fun elFalloConservaElViajeYElPagoSinNavegarYPermiteReintentar() = runTest(dispatcher) {
        conPago(); vm.cargar("TR-1051"); runCurrent(); repos.fallo = ErrorApp.SinConexion
        vm.confirmar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertFalse(vm.estado.value.cancelada)
        assertEquals("TR-1051", vm.estado.value.viaje?.folio); assertNotNull(vm.estado.value.pago)
        assertFalse(vm.estado.value.enviando)
        repos.fallo = null; vm.reintentar(); runCurrent(); vm.confirmar(); runCurrent()
        assertTrue(vm.estado.value.cancelada)
    }

    @Test fun unaSolicitudVencidaNoSeEnvia() = runTest(dispatcher) {
        repos.viajeActual.value = repos.viajeActual.value!!.copy(fhAgenda = ahora.minusHours(1).minusSeconds(1))
        vm.cargar("TR-1051"); runCurrent(); vm.confirmar(); runCurrent()
        assertEquals(0, repos.cancelaciones); assertTrue(vm.estado.value.error is ErrorApp.Conflicto)
    }

    @Test fun unCambioDeEstadoMientrasConfirmaImpideLaCancelacion() = runTest(dispatcher) {
        vm.cargar("TR-1051"); runCurrent()
        repos.viajeActual.value = repos.viajeActual.value!!.copy(estado = EstadoViaje.EN_CURSO); runCurrent()
        vm.confirmar(); runCurrent()
        assertEquals(0, repos.cancelaciones); assertFalse(vm.estado.value.cancelada)
    }

    @Test fun elFalloAlConsultarElPagoQuedaVisible() = runTest(dispatcher) {
        conPago(); repos.errorPago = ErrorApp.SinConexion; vm.cargar("TR-1051"); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertNull(vm.estado.value.pago); assertFalse(vm.estado.value.cargando)
    }

    @Test fun unaSolicitudInexistenteSeInformaComoNoEncontrada() = runTest(dispatcher) {
        repos.viajeActual.value = null; vm.cargar("TR-9999"); runCurrent()
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
        vm.confirmar(); runCurrent()
        assertEquals(0, repos.cancelaciones)
    }
}
