package cl.belmontespa.myapplication.viewmodel.administrador

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.viewmodel.ReposP1Falsos
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import java.time.LocalDateTime
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
class DetallePagoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: DetallePagoViewModel
    private val hash = "0123456789abcdef0123456789abcdef"

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        repos.pago = Pago(hash, "TR-1051", 50_000, EstadoPago.PENDIENTE, "Banco", 12,
            "12345678-5", MedioPago.EFECTIVO, TipoRecibo.BOLETA, null, null)
        vm = DetallePagoViewModel(aplicacionFalsa(), repos.pagos, repos.cuentas)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun dobleToqueEnviaUnaVezYAplicaLaRespuestaSinRecargar() = runTest(dispatcher) {
        vm.cargar(hash); runCurrent(); repos.espera = CompletableDeferred()
        vm.cambiarEstado(EstadoPago.PAGADO); vm.cambiarEstado(EstadoPago.PAGADO); runCurrent()
        assertEquals(1, repos.cambiosPago); assertTrue(vm.estado.value.enviando)
        repos.errorPago = ErrorApp.SinConexion
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(repos.pago, vm.estado.value.pago); assertNotNull(vm.estado.value.pago!!.fechaPago)
        assertEquals(R.string.pago_estado_guardado, vm.estado.value.aviso)
        assertFalse(vm.estado.value.enviando); assertNull(vm.estado.value.error)
        vm.cambiarEstado(EstadoPago.PAGADO); runCurrent()
        assertEquals(1, repos.cambiosPago); assertTrue(vm.estado.value.error is ErrorApp.Conflicto)
    }

    @Test fun pagadoSoloPermiteAnularYConservaLaFecha() = runTest(dispatcher) {
        repos.pago = repos.pago!!.copy(estado = EstadoPago.PAGADO, fechaPago = LocalDateTime.of(2026, 10, 22, 10, 0))
        vm.cargar(hash); runCurrent()
        vm.cambiarEstado(EstadoPago.RECHAZADO); runCurrent(); assertEquals(0, repos.cambiosPago)
        vm.cambiarEstado(EstadoPago.ANULADO); runCurrent()
        assertEquals(EstadoPago.ANULADO, vm.estado.value.pago!!.estado)
        assertEquals(LocalDateTime.of(2026, 10, 22, 10, 0), vm.estado.value.pago!!.fechaPago)
        assertNull(vm.estado.value.pago!!.montoReembolsado)
    }

    @Test fun losEstadosTerminalesNoSeEnvianAlRepositorio() = runTest(dispatcher) {
        for (estado in listOf(EstadoPago.RECHAZADO, EstadoPago.ANULADO, EstadoPago.REEMBOLSADO)) {
            repos.pago = repos.pago!!.copy(estado = estado); vm.cargar(hash, forzar = true); runCurrent()
            for (nuevo in EstadoPago.entries) { vm.cambiarEstado(nuevo); runCurrent() }
        }
        assertEquals(0, repos.cambiosPago)
    }

    @Test fun elFalloConservaElPagoYLaRecargaTraeElEstadoDelServidor() = runTest(dispatcher) {
        vm.cargar(hash); runCurrent(); val original = repos.pago
        repos.fallo = ErrorApp.Conflicto("Otro administrador ya modificó el pago.")
        vm.cambiarEstado(EstadoPago.ANULADO); runCurrent()
        assertEquals(original, vm.estado.value.pago); assertEquals(repos.fallo, vm.estado.value.error)
        assertFalse(vm.estado.value.enviando); assertNull(vm.estado.value.aviso)
        repos.fallo = null; repos.pago = original!!.copy(estado = EstadoPago.RECHAZADO)
        vm.reintentar(); runCurrent()
        assertEquals(EstadoPago.RECHAZADO, vm.estado.value.pago!!.estado); assertNull(vm.estado.value.error)
    }

    @Test fun unSolicitanteNoCambiaElPagoAunqueInvoqueLaAccion() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.SOLICITANTE)
        vm.cargar(hash); runCurrent(); vm.cambiarEstado(EstadoPago.PAGADO); runCurrent()
        assertEquals(0, repos.cambiosPago); assertTrue(vm.estado.value.error is ErrorApp.SinPermiso)
        assertFalse(vm.estado.value.esAdministrador)
    }

    @Test fun unSolicitanteConUnPagoIlegibleNoVeElPagoAnterior() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.SOLICITANTE)
        vm.cargar(hash); runCurrent(); assertNotNull(vm.estado.value.pago)
        repos.errorPago = ErrorApp.SinPermiso(); vm.reintentar(); runCurrent()
        assertNull(vm.estado.value.pago); assertTrue(vm.estado.value.error is ErrorApp.SinPermiso)
    }

    @Test fun sinPagoCargadoNoSeEnvia() = runTest(dispatcher) {
        repos.pago = null; vm.cargar(hash); runCurrent(); vm.cambiarEstado(EstadoPago.ANULADO); runCurrent()
        assertEquals(0, repos.cambiosPago); assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
    }
}
