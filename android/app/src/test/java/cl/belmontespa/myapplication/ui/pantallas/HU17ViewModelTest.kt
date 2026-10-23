package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU17ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, repos,
            Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"), ZoneId.of("America/Santiago")), BorradorAbordaje(), emptyList()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun formulario() {
        vm.cargar(PantallaDemo.REGISTRAR_VEHICULO, "")
        vm.campo("patente", " ab-cd 12 "); vm.campo("capacidad", "20")
    }
    @Test fun rechazaFormatoYCapacidadSinEnviarYConservaLosDatos() = runTest(dispatcher) {
        formulario(); runCurrent()
        for (capacidad in listOf("", "abc", "2147483648", "0", "21")) {
            vm.campo("patente", "ABC123"); vm.campo("capacidad", capacidad)
            vm.ejecutar("guardar"); runCurrent()
            assertEquals(setOf("patente", "capacidad"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
            assertEquals(capacidad, vm.valor("capacidad")); assertTrue(vm.estado.value.modificado)
        }
        assertEquals(0, repos.enviosMovil)
    }
    @Test fun gpsInvalidoNoSeConvierteEnAusente() = runTest(dispatcher) {
        formulario(); runCurrent()
        for (gps in listOf("abc", "0", "-1", "1.5", "2147483648", "99999999999999999999999")) {
            vm.campo("gps", gps); vm.ejecutar("guardar"); runCurrent()
            assertEquals(setOf("gps"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        }
        assertEquals(0, repos.enviosMovil)
    }
    @Test fun normalizaPatenteAceptaGpsVacioYBloqueaDobleEnvio() = runTest(dispatcher) {
        formulario(); runCurrent(); repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.enviosMovil); assertTrue(vm.estado.value.enviando)
        assertEquals(NuevoMovil("ABCD12", 20, null), repos.nuevoMovil)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(PantallaDemo.DETALLE_VEHICULO to "ABCD12", vm.estado.value.destino)
        assertFalse(vm.estado.value.modificado)
    }
    @Test fun conflictoConservaFormularioYPermiteCorregirLaPatente() = runTest(dispatcher) {
        formulario(); runCurrent(); vm.campo("gps", "2147483647")
        repos.fallo = ErrorApp.Conflicto("La patente ya está registrada.")
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(repos.fallo, vm.estado.value.error); assertNull(vm.estado.value.destino)
        assertEquals("2147483647", vm.valor("gps")); assertTrue(vm.estado.value.modificado)
        repos.fallo = null; vm.campo("patente", "zz-1234"); vm.campo("capacidad", "1")
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(NuevoMovil("ZZ1234", 1, 2147483647L), repos.nuevoMovil)
        assertEquals(PantallaDemo.DETALLE_VEHICULO to "ZZ1234", vm.estado.value.destino)
    }
    @Test fun detalleYListaLeenElVehiculoRegistradoConSuCodigo() = runTest(dispatcher) {
        formulario(); runCurrent(); vm.ejecutar("guardar"); runCurrent()
        vm.cargar(PantallaDemo.DETALLE_VEHICULO, "ABCD12"); runCurrent()
        assertEquals("0123456789ABCDEF", vm.estado.value.movil?.codigoAbordaje)
        assertEquals(EstadoMovil.DISPONIBLE, vm.estado.value.movil?.estado)
        vm.cargar(PantallaDemo.VEHICULOS, "", forzar = true); runCurrent()
        assertEquals(listOf("ABCD12"), vm.estado.value.moviles.map { it.patente })
    }
}
