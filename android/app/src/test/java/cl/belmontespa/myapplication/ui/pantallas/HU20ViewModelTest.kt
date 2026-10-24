package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU20ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    private val hash = "0123456789abcdef0123456789abcdef"
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        repos.pago = Pago(hash, "TR-1051", 50_000, EstadoPago.PENDIENTE, "Banco", 12,
            "12345678-5", MedioPago.EFECTIVO, TipoRecibo.BOLETA, null, null)
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, Clock.systemUTC(), emptyList()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun cargar() { vm.cargar(PantallaDemo.DETALLE_PAGO, hash) }

    @Test fun dobleToqueEnviaUnaVezYAplicaLaRespuestaSinRecargar() = runTest(dispatcher) {
        cargar(); runCurrent(); repos.espera = CompletableDeferred()
        vm.ejecutar("pago", "PAGADO"); vm.ejecutar("pago", "PAGADO"); runCurrent()
        assertEquals(1, repos.cambiosPago); assertTrue(vm.estado.value.enviando)
        repos.errorPago = ErrorApp.SinConexion // Una nueva lectura no debe ocultar el éxito confirmado.
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(repos.pago, vm.estado.value.pago); assertNotNull(vm.estado.value.pago!!.fechaPago)
        assertEquals(R.string.pago_estado_guardado, vm.estado.value.aviso)
        assertFalse(vm.estado.value.enviando); assertNull(vm.estado.value.error)
        vm.ejecutar("pago", "PAGADO"); runCurrent()
        assertEquals(1, repos.cambiosPago); assertTrue(vm.estado.value.error is ErrorApp.Conflicto)
    }
    @Test fun pagadoSoloPermiteAnularYConservaLaFecha() = runTest(dispatcher) {
        repos.pago = repos.pago!!.copy(estado = EstadoPago.PAGADO, fechaPago = LocalDateTime.of(2026, 10, 22, 10, 0))
        cargar(); runCurrent()
        vm.ejecutar("pago", "RECHAZADO"); runCurrent(); assertEquals(0, repos.cambiosPago)
        vm.ejecutar("pago", "ANULADO"); runCurrent()
        assertEquals(EstadoPago.ANULADO, vm.estado.value.pago!!.estado)
        assertEquals(LocalDateTime.of(2026, 10, 22, 10, 0), vm.estado.value.pago!!.fechaPago)
        assertNull(vm.estado.value.pago!!.montoReembolsado)
    }
    @Test fun estadosTerminalesNoSeEnvianAlRepositorio() = runTest(dispatcher) {
        for (estado in listOf(EstadoPago.RECHAZADO, EstadoPago.ANULADO, EstadoPago.REEMBOLSADO)) {
            repos.pago = repos.pago!!.copy(estado = estado); vm.cargar(PantallaDemo.DETALLE_PAGO, hash, true); runCurrent()
            for (nuevo in EstadoPago.entries) { vm.ejecutar("pago", nuevo.name); runCurrent() }
        }
        assertEquals(0, repos.cambiosPago)
    }
    @Test fun falloConservaElPagoYRecargaElEstadoDelServidor() = runTest(dispatcher) {
        cargar(); runCurrent(); val original = repos.pago
        repos.fallo = ErrorApp.Conflicto("Otro administrador ya modificó el pago.")
        vm.ejecutar("pago", "ANULADO"); runCurrent()
        assertEquals(original, vm.estado.value.pago); assertEquals(repos.fallo, vm.estado.value.error)
        assertFalse(vm.estado.value.enviando); assertNull(vm.estado.value.aviso)
        repos.fallo = null; repos.pago = original!!.copy(estado = EstadoPago.RECHAZADO)
        vm.actualizar(); runCurrent()
        assertEquals(EstadoPago.RECHAZADO, vm.estado.value.pago!!.estado); assertNull(vm.estado.value.error)
    }
    @Test fun unSolicitanteNoCambiaElPagoAunqueInvoqueLaAccion() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.SOLICITANTE)
        cargar(); runCurrent(); vm.ejecutar("pago", "PAGADO"); runCurrent()
        assertEquals(0, repos.cambiosPago); assertTrue(vm.estado.value.error is ErrorApp.SinPermiso)
    }
    @Test fun sinPagoCargadoNoSeEnvia() = runTest(dispatcher) {
        repos.pago = null; cargar(); runCurrent(); vm.ejecutar("pago", "ANULADO"); runCurrent()
        assertEquals(0, repos.cambiosPago); assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
    }
}
