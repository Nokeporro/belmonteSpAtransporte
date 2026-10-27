package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.model.Banco
import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.model.viajeDePrueba
import cl.belmontespa.myapplication.ui.ReposP1Falsos
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
class RegistrarPagoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: RegistrarPagoViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.viajeActual.value = viajeDePrueba(LocalDateTime.of(2026, 10, 26, 12, 0)).copy(hashPago = null, estadoPago = null)
        repos.catalogosPago = Catalogos(listOf(Banco("Banco", 12)), listOf(MedioPago.EFECTIVO), listOf(TipoRecibo.FACTURA), emptyList())
        vm = RegistrarPagoViewModel(repos, repos, repos)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun datos() {
        vm.escribirMonto("50000"); vm.elegirBanco("12")
        vm.elegirMedioPago("EFECTIVO"); vm.elegirTipoRecibo("FACTURA")
    }

    @Test fun cargaLosCatalogosYElViajeDelRegistro() = runTest(dispatcher) {
        vm.cargar("TR-1051"); runCurrent()
        assertEquals(repos.catalogosPago, vm.estado.value.catalogos)
        assertEquals(repos.viajeActual.value, vm.estado.value.viaje)
        assertTrue(vm.estado.value.viajeAdmitePago); assertNull(vm.estado.value.error)
    }

    @Test fun unMontoNoEnteroNoPositivoOFueraDeRangoNoSeEnvia() = runTest(dispatcher) {
        vm.cargar("TR-1051"); runCurrent(); datos()
        for (monto in listOf("", "abc", "1.5", "0", "-1", "2147483648")) {
            vm.escribirMonto(monto); vm.guardar(); runCurrent()
            assertEquals(setOf("monto"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
            assertEquals(monto, vm.estado.value.monto)
        }
        assertEquals(0, repos.enviosPago)
    }

    @Test fun validaLasOpcionesContraLosCatalogosRecibidos() = runTest(dispatcher) {
        vm.cargar("TR-1051"); runCurrent(); datos()
        vm.elegirBanco("999"); vm.elegirMedioPago("TRANSFERENCIA"); vm.elegirTipoRecibo("BOLETA")
        vm.guardar(); runCurrent()
        assertEquals(
            setOf("banco", "medioPago", "tipoRecibo"),
            (vm.estado.value.error as ErrorApp.Validacion).errores.keys,
        )
        assertEquals(0, repos.enviosPago)
    }

    @Test fun dobleToqueSoloEnviaUnaVezYNavegaAlPagoPendiente() = runTest(dispatcher) {
        vm.cargar("TR-1051"); runCurrent(); datos(); repos.espera = CompletableDeferred()
        vm.guardar(); vm.guardar(); runCurrent()
        assertEquals(1, repos.enviosPago); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(NuevoPago("TR-1051", 50000, 12, MedioPago.EFECTIVO, TipoRecibo.FACTURA), repos.nuevoPago)
        assertEquals(repos.pago!!.hash, vm.estado.value.registrado)
        assertNull(repos.pago!!.fechaPago); assertFalse(vm.estado.value.modificado)
    }

    @Test fun elErrorConservaLosDatosYPermiteReintentar() = runTest(dispatcher) {
        vm.cargar("TR-1051"); runCurrent(); datos(); repos.fallo = ErrorApp.SinConexion
        vm.guardar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertEquals("50000", vm.estado.value.monto)
        assertTrue(vm.estado.value.modificado); assertNull(vm.estado.value.registrado)
        repos.fallo = null; vm.guardar(); runCurrent()
        assertNotNull(vm.estado.value.registrado)
    }

    @Test fun losCambiosDelViajeBloqueanUnPagoDuplicadoOUnViajeCancelado() = runTest(dispatcher) {
        vm.cargar("TR-1051"); runCurrent(); datos()
        val original = repos.viajeActual.value!!
        for (viaje in listOf(original.copy(estado = EstadoViaje.CANCELADO), original.copy(hashPago = "otro-pago"))) {
            repos.viajeActual.value = viaje; runCurrent()
            assertFalse(vm.estado.value.viajeAdmitePago)
            vm.guardar(); runCurrent()
            assertTrue(vm.estado.value.error is ErrorApp.Conflicto)
        }
        assertEquals(0, repos.enviosPago)
    }

    @Test fun elFalloDelCatalogoEsVisibleYLaRecargaConservaElMonto() = runTest(dispatcher) {
        repos.errorCatalogos = ErrorApp.SinConexion; vm.cargar("TR-1051"); runCurrent(); datos()
        vm.reintentar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertEquals("50000", vm.estado.value.monto)
        repos.errorCatalogos = null; vm.reintentar(); runCurrent(); vm.guardar(); runCurrent()
        assertEquals(1, repos.enviosPago)
    }
}
