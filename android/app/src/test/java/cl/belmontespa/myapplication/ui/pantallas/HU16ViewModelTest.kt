package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU16ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"), ZoneId.of("America/Santiago"))
    private val ahora = LocalDateTime.now(clock)
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.viajeActual.value = viajeDePrueba(ahora.plusHours(3)).copy(correoSolicitante = "cuenta@test.cl")
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, clock))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun cargar() { vm.cargar(PantallaDemo.CONFIRMAR_CANCELACION, "TR-1051") }
    private fun conPago(estado: EstadoPago = EstadoPago.PAGADO) {
        repos.pago = Pago("pago-prueba", "TR-1051", 50_001, estado, "Banco", 12, "12345678-5", MedioPago.TRANSFERENCIA, TipoRecibo.BOLETA, ahora.minusDays(1), null)
        repos.viajeActual.value = repos.viajeActual.value!!.copy(hashPago = "pago-prueba", estadoPago = estado)
    }
    @Test fun cargaElPagoAntesDeConfirmar() = runTest(dispatcher) {
        conPago(); cargar(); runCurrent()
        assertEquals(50_001, vm.estado.value.pago?.monto)
        assertEquals(EstadoPago.PAGADO, vm.estado.value.pago?.estado)
        assertNull(vm.estado.value.error)
    }
    @Test fun dobleToqueCancelaUnaVezYSoloEnviaElFolio() = runTest(dispatcher) {
        conPago(); cargar(); runCurrent()
        vm.campo("motivo", "Dato que no debe enviarse"); vm.campo("reembolso", "50001")
        repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.cancelaciones); assertTrue(vm.estado.value.enviando)
        assertNull(repos.motivoCancelacion); assertNull(repos.montoCancelacion)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(PantallaDemo.SOLICITUD_CANCELADA to "TR-1051", vm.estado.value.destino)
    }
    @Test fun falloConservaElViajeYElPagoSinNavegarYPermiteReintentar() = runTest(dispatcher) {
        conPago(); cargar(); runCurrent(); repos.fallo = ErrorApp.SinConexion
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertNull(vm.estado.value.destino)
        assertEquals("TR-1051", vm.estado.value.viaje?.folio); assertNotNull(vm.estado.value.pago)
        assertFalse(vm.estado.value.enviando)
        repos.fallo = null; vm.actualizar(); runCurrent(); vm.ejecutar("guardar"); runCurrent()
        assertEquals(PantallaDemo.SOLICITUD_CANCELADA to "TR-1051", vm.estado.value.destino)
    }
    @Test fun unaSolicitudVencidaNoSeEnvia() = runTest(dispatcher) {
        repos.viajeActual.value = repos.viajeActual.value!!.copy(fhAgenda = ahora.minusHours(1).minusSeconds(1))
        cargar(); runCurrent(); vm.ejecutar("guardar"); runCurrent()
        assertEquals(0, repos.cancelaciones); assertTrue(vm.estado.value.error is ErrorApp.Conflicto)
    }
    @Test fun unCambioDeEstadoMientrasConfirmaImpideLaCancelacion() = runTest(dispatcher) {
        cargar(); runCurrent()
        repos.viajeActual.value = repos.viajeActual.value!!.copy(estado = EstadoViaje.EN_CURSO); runCurrent()
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(0, repos.cancelaciones); assertNull(vm.estado.value.destino)
    }
    @Test fun falloAlConsultarElPagoQuedaVisible() = runTest(dispatcher) {
        conPago(); repos.errorPago = ErrorApp.SinConexion; cargar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertNull(vm.estado.value.pago); assertFalse(vm.estado.value.cargando)
    }
    @Test fun elResultadoCargaElMontoReembolsadoPersistido() = runTest(dispatcher) {
        conPago(EstadoPago.REEMBOLSADO)
        repos.pago = repos.pago!!.copy(montoReembolsado = 25_000)
        repos.viajeActual.value = repos.viajeActual.value!!.copy(estado = EstadoViaje.CANCELADO)
        vm.cargar(PantallaDemo.SOLICITUD_CANCELADA, "TR-1051"); runCurrent()
        assertEquals(25_000, vm.estado.value.pago?.montoReembolsado)
        assertEquals(EstadoViaje.CANCELADO, vm.estado.value.viaje?.estado)
    }
}
