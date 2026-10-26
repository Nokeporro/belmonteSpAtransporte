package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU19ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.viajeActual.value = viajeDePrueba(LocalDateTime.of(2026, 10, 26, 12, 0)).copy(hashPago = null, estadoPago = null)
        repos.catalogosPago = Catalogos(listOf(Banco("Banco", 12)), listOf(MedioPago.EFECTIVO), listOf(TipoRecibo.FACTURA), emptyList())
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, Clock.systemUTC()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun cargar() { vm.cargar(PantallaDemo.REGISTRAR_PAGO, "TR-1051") }
    private fun datos() {
        vm.campo("monto", "50000"); vm.campo("banco", "12")
        vm.campo("medioPago", "EFECTIVO"); vm.campo("tipoRecibo", "FACTURA")
    }
    @Test fun cargaLosCatalogosYElViajeDelRegistro() = runTest(dispatcher) {
        cargar(); runCurrent()
        assertEquals(repos.catalogosPago, vm.estado.value.catalogos)
        assertEquals(repos.viajeActual.value, vm.estado.value.viaje)
        assertNull(vm.estado.value.error)
    }
    @Test fun montoNoEnteroNoPositivoOFueraDeRangoNoSeEnvia() = runTest(dispatcher) {
        cargar(); runCurrent(); datos()
        for (monto in listOf("", "abc", "1.5", "0", "-1", "2147483648")) {
            vm.campo("monto", monto); vm.ejecutar("guardar"); runCurrent()
            assertEquals(setOf("monto"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
            assertEquals(monto, vm.valor("monto"))
        }
        assertEquals(0, repos.enviosPago)
    }
    @Test fun validaLasOpcionesContraLosCatalogosRecibidos() = runTest(dispatcher) {
        cargar(); runCurrent(); datos()
        vm.campo("banco", "999"); vm.campo("medioPago", "TRANSFERENCIA"); vm.campo("tipoRecibo", "BOLETA")
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(setOf("banco", "medioPago", "tipoRecibo"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.enviosPago)
    }
    @Test fun dobleToqueSoloEnviaUnaVezYNavegaAlPagoPendiente() = runTest(dispatcher) {
        cargar(); runCurrent(); datos(); repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.enviosPago); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(NuevoPago("TR-1051", 50000, 12, MedioPago.EFECTIVO, TipoRecibo.FACTURA), repos.nuevoPago)
        assertEquals(PantallaDemo.DETALLE_PAGO to repos.pago!!.hash, vm.estado.value.destino)
        assertNull(repos.pago!!.fechaPago); assertFalse(vm.estado.value.modificado)
    }
    @Test fun errorConservaLosDatosYPermiteReintentar() = runTest(dispatcher) {
        cargar(); runCurrent(); datos(); repos.fallo = ErrorApp.SinConexion
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertEquals("50000", vm.valor("monto"))
        assertTrue(vm.estado.value.modificado); assertNull(vm.estado.value.destino)
        repos.fallo = null; vm.ejecutar("guardar"); runCurrent()
        assertNotNull(vm.estado.value.destino)
    }
    @Test fun cambiosDelViajeBloqueanPagoDuplicadoOCancelado() = runTest(dispatcher) {
        cargar(); runCurrent(); datos()
        val original = repos.viajeActual.value!!
        for (viaje in listOf(original.copy(estado = EstadoViaje.CANCELADO), original.copy(hashPago = "otro-pago"))) {
            repos.viajeActual.value = viaje; runCurrent(); vm.ejecutar("guardar"); runCurrent()
            assertTrue(vm.estado.value.error is ErrorApp.Conflicto)
        }
        assertEquals(0, repos.enviosPago)
    }
    @Test fun falloDelCatalogoEsVisibleYLaRecargaConservaElMonto() = runTest(dispatcher) {
        repos.errorCatalogos = ErrorApp.SinConexion; cargar(); runCurrent(); datos()
        vm.actualizar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertEquals("50000", vm.valor("monto"))
        repos.errorCatalogos = null; vm.actualizar(); runCurrent(); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.enviosPago)
    }
}
