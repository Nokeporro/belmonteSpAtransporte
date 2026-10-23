package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU15ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), ZoneId.of("America/Santiago"))
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, repos, clock, BorradorAbordaje(), emptyList()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun formulario() {
        vm.campo("nombre", "Ana Conductora"); vm.campo("correo", "ana@test.cl")
        vm.campo("telefono", "9 5555-0101"); vm.campo("password", "Clave12345")
    }
    @Test fun altaInvalidaNoLlamaAlRepositorioYMuestraErroresPorCampo() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.REGISTRAR_CONDUCTOR, ""); runCurrent()
        vm.campo("nombre", "A"); vm.campo("correo", "invalido"); vm.campo("password", "abcdefgh"); vm.campo("telefono", "123")
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(setOf("nombre", "correo", "telefono", "password"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.enviosConductor)
        assertTrue(vm.estado.value.modificado)
    }
    @Test fun altaBloqueaDobleEnvioYBorraLaClaveAntesDeNavegarAlDetalle() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.REGISTRAR_CONDUCTOR, ""); runCurrent(); formulario()
        repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.enviosConductor); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(PantallaDemo.DETALLE_CONDUCTOR to repos.conductor.correo, vm.estado.value.destino)
        assertEquals("", vm.valor("password")); assertFalse(vm.estado.value.modificado)
    }
    @Test fun conflictoConservaDatosYPermiteCorregirElCorreo() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.REGISTRAR_CONDUCTOR, ""); runCurrent(); formulario()
        repos.fallo = ErrorApp.Conflicto("El correo ya está registrado.")
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(repos.fallo, vm.estado.value.error)
        assertEquals("Clave12345", vm.valor("password")); assertNull(vm.estado.value.destino)
        vm.campo("correo", "otra@test.cl"); repos.fallo = null
        vm.ejecutar("guardar"); runCurrent()
        assertEquals("otra@test.cl", repos.cuentaConductor?.correo)
        assertNotNull(vm.estado.value.destino)
    }
    @Test fun editarSinClaveConservaLaActualYActualizaElDetalle() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.DETALLE_CONDUCTOR, repos.conductor.correo); runCurrent()
        vm.campo("nombre", "  Nombre Editado  ")
        vm.ejecutar("guardar"); runCurrent()
        assertNull(repos.passwordConductor)
        assertEquals("Nombre Editado", vm.estado.value.conductor?.nombre)
        assertEquals("Nombre Editado", vm.valor("nombre"))
        assertFalse(vm.estado.value.modificado)
    }
    @Test fun falloDeEdicionYRecargaNoPierdenLosDatosDelAdministrador() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.DETALLE_CONDUCTOR, repos.conductor.correo); runCurrent()
        vm.campo("nombre", "Nombre sin guardar"); vm.campo("password", "NuevaClave123")
        repos.fallo = ErrorApp.SinConexion
        vm.ejecutar("guardar"); runCurrent(); vm.actualizar(); runCurrent()
        assertEquals("Nombre sin guardar", vm.valor("nombre")); assertEquals("NuevaClave123", vm.valor("password"))
        assertTrue(vm.estado.value.modificado)
        repos.fallo = null; vm.ejecutar("guardar"); runCurrent()
        assertEquals("NuevaClave123", repos.passwordConductor)
        assertEquals("", vm.valor("password"))
    }
    @Test fun unaClaveDeMasDe72BytesNoSeEnvia() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.REGISTRAR_CONDUCTOR, ""); runCurrent(); formulario()
        vm.campo("password", "é".repeat(36) + "1")
        vm.ejecutar("guardar"); runCurrent()
        assertTrue("password" in (vm.estado.value.error as ErrorApp.Validacion).errores)
        assertEquals(0, repos.enviosConductor)
    }
}
