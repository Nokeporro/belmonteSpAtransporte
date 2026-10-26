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
class HU22ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher); repos = ReposP1Falsos()
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, Clock.systemUTC()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun datos(actual: String = "Temporal1234", nueva: String = "NuevaClave123", confirmacion: String = nueva) {
        vm.campo("passwordActual", actual); vm.campo("passwordNueva", nueva); vm.campo("confirmacion", confirmacion)
    }
    @Test fun correoInvalidoNoSeEnvia() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.RECUPERAR, ""); runCurrent()
        for (correo in listOf("", "no-es-correo", "a b@test.cl", "a".repeat(145) + "@test.cl")) {
            vm.campo("correo", correo); vm.ejecutar("guardar"); runCurrent()
            assertEquals(setOf("correo"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        }
        assertEquals(0, repos.recuperaciones)
    }
    @Test fun recuperacionNormalizaCorreoYBloqueaDobleToque() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.RECUPERAR, ""); runCurrent(); vm.campo("correo", "  ANA@Test.CL  ")
        repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.recuperaciones); assertEquals("ana@test.cl", repos.correoRecuperacion)
        assertTrue(vm.estado.value.enviando); assertNull(vm.estado.value.aviso)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(R.string.aviso_recuperacion_enviada, vm.estado.value.aviso); assertFalse(vm.estado.value.modificado)
    }
    @Test fun sinConexionConservaCorreoYNoMuestraConfirmacion() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.RECUPERAR, ""); runCurrent(); vm.campo("correo", "ana@test.cl")
        repos.fallo = ErrorApp.SinConexion; vm.ejecutar("guardar"); runCurrent()
        assertEquals("ana@test.cl", vm.valor("correo")); assertEquals(repos.fallo, vm.estado.value.error)
        assertNull(vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
        repos.fallo = null; vm.ejecutar("guardar"); runCurrent(); assertEquals(R.string.aviso_recuperacion_enviada, vm.estado.value.aviso)
    }
    @Test fun validaCamposDeCambioIncluidoElLimiteDeBytes() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.CAMBIO_OBLIGATORIO, ""); runCurrent(); datos("", "soloLetras", "distinta")
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(setOf("passwordActual", "passwordNueva", "confirmacion"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        datos(nueva = "á".repeat(36) + "1"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(setOf("passwordNueva"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.cambiosPassword)
    }
    @Test fun errorDeClaveActualConservaCamposYExitoLosBorraSinRepetirEnvio() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.CAMBIO_OBLIGATORIO, ""); runCurrent(); datos()
        repos.fallo = ErrorApp.Validacion(mapOf("passwordActual" to "No coincide."))
        vm.ejecutar("guardar"); runCurrent(); assertEquals("Temporal1234", vm.valor("passwordActual"))
        assertEquals("NuevaClave123", vm.valor("passwordNueva")); assertNull(vm.estado.value.aviso)
        repos.fallo = null; repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent(); assertEquals(2, repos.cambiosPassword)
        repos.espera!!.complete(Unit); runCurrent()
        for (campo in listOf("passwordActual", "passwordNueva", "confirmacion")) assertEquals("", vm.valor(campo))
        assertEquals("Temporal1234" to "NuevaClave123", repos.clavesCambiadas)
        assertEquals(R.string.aviso_password_guardada, vm.estado.value.aviso)
    }
    @Test fun conductorNoPuedeCambiarClave() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.CONDUCTOR)
        vm.cargar(PantallaDemo.CAMBIAR_PASSWORD, ""); runCurrent(); datos(); vm.ejecutar("guardar"); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.cambiosPassword)
    }
}
