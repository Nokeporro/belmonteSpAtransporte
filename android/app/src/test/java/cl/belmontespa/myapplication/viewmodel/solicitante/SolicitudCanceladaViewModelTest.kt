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
class SolicitudCanceladaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 10, 23, 12, 0)
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: SolicitudCanceladaViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = SolicitudCanceladaViewModel(aplicacionFalsa(), repos.viajes, repos.pagos)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun cargaElMontoReembolsadoPersistido() = runTest(dispatcher) {
        repos.pago = Pago("pago-prueba", "TR-1051", 50_001, EstadoPago.REEMBOLSADO, "Banco", 12, "12345678-5",
            MedioPago.TRANSFERENCIA, TipoRecibo.BOLETA, ahora.minusDays(1), 25_000)
        repos.viajeActual.value = viajeDePrueba(ahora.plusHours(3), estado = EstadoViaje.CANCELADO)
            .copy(hashPago = "pago-prueba", estadoPago = EstadoPago.REEMBOLSADO)
        vm.cargar("TR-1051"); runCurrent()
        assertEquals(25_000, vm.estado.value.pago?.montoReembolsado)
        assertEquals(EstadoViaje.CANCELADO, vm.estado.value.viaje?.estado)
    }

    @Test fun elFalloAlLeerElPagoQuedaVisibleYSePuedeReintentar() = runTest(dispatcher) {
        repos.viajeActual.value = viajeDePrueba(ahora.plusHours(3), estado = EstadoViaje.CANCELADO)
            .copy(hashPago = "pago-prueba", estadoPago = EstadoPago.ANULADO)
        repos.errorPago = ErrorApp.SinConexion
        vm.cargar("TR-1051"); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertFalse(vm.estado.value.cargando)
        repos.errorPago = null
        repos.pago = Pago("pago-prueba", "TR-1051", 50_001, EstadoPago.ANULADO, "Banco", 12, "12345678-5",
            MedioPago.TRANSFERENCIA, TipoRecibo.BOLETA, null, null)
        vm.reintentar(); runCurrent()
        assertNull(vm.estado.value.error); assertEquals(EstadoPago.ANULADO, vm.estado.value.pago?.estado)
    }
}
