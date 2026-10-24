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
class HU28ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher); repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, Clock.systemUTC(), emptyList()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun formulario() {
        vm.campo("nombre", "  Nueva Administradora  "); vm.campo("correo", "  ADMIN@TEST.CL  ")
        vm.campo("telefono", "9 5555-0101"); vm.campo("password", "Inicial12345")
    }
    @Test fun erroresPorCampoNoEnvianElAlta() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.REGISTRAR_ADMIN, ""); runCurrent()
        vm.campo("nombre", "A"); vm.campo("correo", "invalido"); vm.campo("telefono", "123"); vm.campo("password", "abcdefgh")
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(setOf("nombre", "correo", "telefono", "password"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.enviosAdministrador)
    }
    @Test fun dobleToqueSoloGuardaUnaVezYLimpiaLaClaveSinCambiarLaSesion() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.REGISTRAR_ADMIN, ""); runCurrent(); formulario(); repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.enviosAdministrador); assertTrue(vm.estado.value.enviando)
        assertEquals("Inicial12345", vm.valor("password")); assertNull(vm.estado.value.aviso)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("", vm.valor("password")); assertEquals("admin@test.cl", vm.valor("correo"))
        assertEquals("Nueva Administradora", vm.valor("nombre")); assertEquals("+56955550101", vm.valor("telefono"))
        assertEquals(R.string.admin_alta_guardada, vm.estado.value.aviso); assertFalse(vm.estado.value.modificado)
        assertEquals(repos.perfil, vm.estado.value.cuenta); assertNull(vm.estado.value.destino)
    }
    @Test fun conflictoYDesconexionConservanDatosParaCorregirYReintentar() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.REGISTRAR_ADMIN, ""); runCurrent(); formulario()
        for (error in listOf(ErrorApp.Conflicto("El correo ya está registrado."), ErrorApp.SinConexion)) {
            repos.fallo = error; vm.ejecutar("guardar"); runCurrent()
            assertEquals(error, vm.estado.value.error); assertEquals("Inicial12345", vm.valor("password"))
            assertNull(vm.estado.value.aviso); assertTrue(vm.estado.value.modificado)
        }
        repos.fallo = null; vm.campo("correo", "otra@test.cl"); vm.ejecutar("guardar"); runCurrent()
        assertEquals("otra@test.cl", repos.cuentaAdministrador!!.correo); assertEquals("", vm.valor("password"))
    }
    @Test fun otroRolNoPuedeCrearUnAdministrador() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.SOLICITANTE)
        vm.cargar(PantallaDemo.REGISTRAR_ADMIN, ""); runCurrent(); formulario(); vm.ejecutar("guardar"); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.enviosAdministrador)
    }
}
