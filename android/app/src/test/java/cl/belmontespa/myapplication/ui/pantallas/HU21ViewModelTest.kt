package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU21ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneId.of("America/Santiago"))
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher); repos = ReposP1Falsos()
        repos.viajeActual.value = viajeDePrueba(LocalDateTime.now(clock).plusHours(3)).copy(hashPago = "pago", estadoPago = EstadoPago.PAGADO)
        repos.pago = Pago("pago", repos.viajeActual.value!!.folio, 50001, EstadoPago.PAGADO, "Banco", 12,
            "12345678-5", MedioPago.TRANSFERENCIA, TipoRecibo.BOLETA, LocalDateTime.now(clock), null)
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, clock))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun cargar() = vm.cargar(PantallaDemo.DETALLE_SOLICITUD, repos.viajeActual.value!!.folio)
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
    @Test fun falloDelPagoConservaSolicitudYObservacionYPermiteReintentar() = runTest(dispatcher) {
        repos.errorPago = ErrorApp.SinConexion; cargar(); runCurrent()
        assertNotNull(vm.estado.value.viaje); assertNull(vm.estado.value.error)
        assertEquals(ErrorApp.SinConexion, vm.estado.value.errorPago); assertNull(vm.estado.value.pago)
        repos.errorPago = null; vm.actualizar(); runCurrent()
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
    @Test fun solicitanteNoPuedeCambiarEstadoDesdeElDetalleDelPago() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.DETALLE_PAGO, "pago"); runCurrent()
        vm.ejecutar("pago", EstadoPago.ANULADO.name); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.cambiosPago)
        repos.errorPago = ErrorApp.SinPermiso(); vm.actualizar(); runCurrent()
        assertNull(vm.estado.value.pago); assertTrue(vm.estado.value.error is ErrorApp.SinPermiso)
    }
}
