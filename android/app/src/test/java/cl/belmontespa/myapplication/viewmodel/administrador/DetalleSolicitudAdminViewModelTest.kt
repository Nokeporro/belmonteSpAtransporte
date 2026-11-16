package cl.belmontespa.myapplication.viewmodel.administrador

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.viajeDePrueba
import cl.belmontespa.myapplication.viewmodel.ReposP1Falsos
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleSolicitudAdminViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: DetalleSolicitudAdminViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.viajeActual.value = viajeDePrueba(LocalDateTime.of(2026, 11, 15, 15, 0))
        vm = DetalleSolicitudAdminViewModel(aplicacionFalsa(), repos.viajes, repos.pagos)
    }
    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun abrirYReintentarConsultanElServidorAntesDeMostrarElDetalle() = runTest(dispatcher) {
        val folio = repos.viajeActual.value!!.folio
        vm.cargar(folio); runCurrent()
        assertNotNull(vm.estado.value.viaje)
        repos.errorRefrescoViaje = ErrorApp.SinConexion
        vm.reintentar(); runCurrent()
        assertEquals(listOf(folio, folio), repos.refrescosViaje)
        assertNull(vm.estado.value.viaje)
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        repos.errorRefrescoViaje = null
        vm.reintentar(); runCurrent()
        assertNotNull(vm.estado.value.viaje)
        assertNull(vm.estado.value.error)
    }
}
