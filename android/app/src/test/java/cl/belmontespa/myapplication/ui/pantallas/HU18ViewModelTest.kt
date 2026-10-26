package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU18ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.moviles.add(Movil("ABCD12", 8, null, true, EstadoMovil.DISPONIBLE, "0123456789ABCDEF"))
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, Clock.systemUTC()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    @Test fun abrirDesdeVehiculoPreseleccionaYFiltraSinMarcarCambios() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.HABILITACIONES, "vehiculo:ABCD12"); runCurrent()
        assertEquals("ABCD12", vm.valor("vehiculo")); assertEquals("ABCD12", vm.valor("filtroVehiculo"))
        assertEquals("ABCD12" to null, repos.filtroHabilitacion); assertFalse(vm.estado.value.modificado)
    }
    @Test fun abrirDesdeConductorPreseleccionaYFiltraSinMarcarCambios() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.HABILITACIONES, "conductor:${repos.conductor.correo}"); runCurrent()
        assertEquals(repos.conductor.correo, vm.valor("conductor"))
        assertEquals(null to repos.conductor.correo, repos.filtroHabilitacion)
        assertFalse(vm.estado.value.modificado)
    }
    @Test fun filtrosCombinadosNoCreanRelacionesNiCambianLaSeleccionDeAlta() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.HABILITACIONES, ""); runCurrent()
        vm.campo("filtroVehiculo", "ABCD12"); vm.campo("filtroConductor", repos.conductor.correo)
        vm.actualizar(); runCurrent()
        assertEquals("ABCD12" to repos.conductor.correo, repos.filtroHabilitacion)
        assertEquals("", vm.valor("vehiculo")); assertEquals("", vm.valor("conductor"))
        assertFalse(vm.estado.value.modificado); assertEquals(0, repos.enviosHabilitacion)
        vm.campo("filtroVehiculo", ""); vm.campo("filtroConductor", ""); vm.actualizar(); runCurrent()
        assertEquals(null to null, repos.filtroHabilitacion)
    }
    @Test fun seleccionVaciaOInactivaNoSeEnvia() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.HABILITACIONES, ""); runCurrent(); vm.ejecutar("guardar"); runCurrent()
        assertEquals(setOf("vehiculo", "conductor"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        repos.moviles[0] = repos.moviles[0].copy(activo = false)
        vm.campo("vehiculo", "ABCD12"); vm.campo("conductor", repos.conductor.correo); vm.actualizar(); runCurrent()
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(setOf("vehiculo"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.enviosHabilitacion)
    }
    @Test fun bloqueaDobleEnvioYRecargaListaSinDuplicarLaRelacion() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.HABILITACIONES, "vehiculo:ABCD12"); runCurrent()
        vm.campo("conductor", repos.conductor.correo); repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.enviosHabilitacion); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(1, vm.estado.value.habilitaciones.size); assertNotNull(vm.estado.value.aviso)
        assertFalse(vm.estado.value.modificado)
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, vm.estado.value.habilitaciones.size)
    }
    @Test fun errorConservaSeleccionYPermiteReintentar() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.HABILITACIONES, "vehiculo:ABCD12"); runCurrent()
        vm.campo("conductor", repos.conductor.correo); repos.fallo = ErrorApp.SinConexion
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(repos.fallo, vm.estado.value.error); assertTrue(vm.estado.value.modificado)
        assertEquals("ABCD12", vm.valor("vehiculo")); assertEquals(repos.conductor.correo, vm.valor("conductor"))
        repos.fallo = null; vm.ejecutar("guardar"); runCurrent()
        assertNull(vm.estado.value.error); assertEquals(1, vm.estado.value.habilitaciones.size)
    }
}
