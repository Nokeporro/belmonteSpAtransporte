package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.ui.ReposP1Falsos
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
class HabilitacionesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: HabilitacionesViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.moviles.add(Movil("ABCD12", 8, null, true, EstadoMovil.DISPONIBLE, "0123456789ABCDEF"))
        vm = HabilitacionesViewModel(repos)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun abrirDesdeUnVehiculoLoPreseleccionaYFiltraSinMarcarCambios() = runTest(dispatcher) {
        vm.cargar("vehiculo:ABCD12"); runCurrent()
        assertEquals("ABCD12", vm.estado.value.vehiculo); assertEquals("ABCD12", vm.estado.value.filtroVehiculo)
        assertEquals("ABCD12" to null, repos.filtroHabilitacion); assertFalse(vm.estado.value.modificado)
    }

    @Test fun abrirDesdeUnConductorLoPreseleccionaYFiltraSinMarcarCambios() = runTest(dispatcher) {
        vm.cargar("conductor:${repos.conductor.correo}"); runCurrent()
        assertEquals(repos.conductor.correo, vm.estado.value.conductor)
        assertEquals(null to repos.conductor.correo, repos.filtroHabilitacion)
        assertFalse(vm.estado.value.modificado)
    }

    @Test fun losFiltrosCombinadosNoCreanRelacionesNiCambianLaSeleccionDeAlta() = runTest(dispatcher) {
        vm.cargar(""); runCurrent()
        vm.filtrarPorVehiculo("ABCD12"); runCurrent()
        vm.filtrarPorConductor(repos.conductor.correo); runCurrent()
        assertEquals("ABCD12" to repos.conductor.correo, repos.filtroHabilitacion)
        assertEquals("", vm.estado.value.vehiculo); assertEquals("", vm.estado.value.conductor)
        assertFalse(vm.estado.value.modificado); assertEquals(0, repos.enviosHabilitacion)
        vm.filtrarPorVehiculo(""); vm.filtrarPorConductor(""); runCurrent()
        assertEquals(null to null, repos.filtroHabilitacion)
    }

    @Test fun unaSeleccionVaciaOInactivaNoSeEnvia() = runTest(dispatcher) {
        vm.cargar(""); runCurrent(); vm.habilitar(); runCurrent()
        assertEquals(setOf("vehiculo", "conductor"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        repos.moviles[0] = repos.moviles[0].copy(activo = false)
        vm.reintentar(); runCurrent()
        vm.elegirVehiculo("ABCD12"); vm.elegirConductor(repos.conductor.correo)
        vm.habilitar(); runCurrent()
        assertEquals(setOf("vehiculo"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.enviosHabilitacion)
    }

    @Test fun bloqueaElDobleEnvioYRecargaLaListaSinDuplicarLaRelacion() = runTest(dispatcher) {
        vm.cargar("vehiculo:ABCD12"); runCurrent()
        vm.elegirConductor(repos.conductor.correo); repos.espera = CompletableDeferred()
        vm.habilitar(); vm.habilitar(); runCurrent()
        assertEquals(1, repos.enviosHabilitacion); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(1, vm.estado.value.habilitaciones.size); assertNotNull(vm.estado.value.aviso)
        assertFalse(vm.estado.value.modificado)
        vm.habilitar(); runCurrent()
        assertEquals(1, vm.estado.value.habilitaciones.size)
    }

    @Test fun elErrorConservaLaSeleccionYPermiteReintentar() = runTest(dispatcher) {
        vm.cargar("vehiculo:ABCD12"); runCurrent()
        vm.elegirConductor(repos.conductor.correo); repos.fallo = ErrorApp.SinConexion
        vm.habilitar(); runCurrent()
        assertEquals(repos.fallo, vm.estado.value.error); assertTrue(vm.estado.value.modificado)
        assertEquals("ABCD12", vm.estado.value.vehiculo); assertEquals(repos.conductor.correo, vm.estado.value.conductor)
        repos.fallo = null; vm.habilitar(); runCurrent()
        assertNull(vm.estado.value.error); assertEquals(1, vm.estado.value.habilitaciones.size)
    }
}
