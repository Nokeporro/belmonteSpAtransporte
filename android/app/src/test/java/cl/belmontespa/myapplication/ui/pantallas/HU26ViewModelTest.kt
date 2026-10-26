package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU26ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher); repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        repos.moviles.add(Movil("ZZ1234", 4, null, true, EstadoMovil.DISPONIBLE, "0123456789ABCDEF"))
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, Clock.systemUTC()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    @Test fun desactivaYReactivaConductorConRespuestaConfirmadaSinPerderEdicion() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.DETALLE_CONDUCTOR, repos.conductor.correo); runCurrent()
        vm.campo("nombre", "Edición pendiente")
        repos.espera = CompletableDeferred()
        vm.ejecutar("activoConductor"); vm.ejecutar("activoConductor"); runCurrent()
        assertTrue(vm.estado.value.conductor!!.activo); assertEquals(1, repos.cambiosActivo)
        repos.espera!!.complete(Unit); runCurrent()
        assertFalse(vm.estado.value.conductor!!.activo); assertEquals("Edición pendiente", vm.valor("nombre"))
        assertEquals(R.string.flota_activo_guardado, vm.estado.value.aviso)
        vm.ejecutar("activoConductor"); runCurrent(); assertTrue(vm.estado.value.conductor!!.activo)
    }
    @Test fun movilConservaQrCapacidadYGps() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.DETALLE_VEHICULO, "ZZ1234"); runCurrent()
        val original = vm.estado.value.movil!!
        vm.ejecutar("activoMovil"); runCurrent(); assertEquals(original.copy(activo = false), vm.estado.value.movil)
        vm.ejecutar("activoMovil"); runCurrent(); assertEquals(original, vm.estado.value.movil)
    }
    @Test fun conflictoYDesconexionNoAfirmanExitoNiCambianElEstado() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.DETALLE_VEHICULO, "ZZ1234"); runCurrent()
        for (error in listOf(ErrorApp.Conflicto("Tiene viajes asignados."), ErrorApp.SinConexion)) {
            repos.fallo = error; vm.ejecutar("activoMovil"); runCurrent()
            assertTrue(vm.estado.value.movil!!.activo); assertEquals(error, vm.estado.value.error)
            assertNull(vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
        }
        repos.fallo = null; vm.actualizar(); runCurrent(); vm.ejecutar("activoMovil"); runCurrent()
        assertFalse(vm.estado.value.movil!!.activo)
    }
    @Test fun soloAdminEnElDetallePuedeCambiarActivo() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.CONDUCTOR)
        vm.cargar(PantallaDemo.DETALLE_CONDUCTOR, repos.conductor.correo); runCurrent()
        vm.ejecutar("activoConductor"); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.cambiosActivo)
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        vm.cargar(PantallaDemo.VEHICULOS, ""); runCurrent(); vm.ejecutar("activoMovil"); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.cambiosActivo)
    }
}
