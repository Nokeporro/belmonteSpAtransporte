package cl.belmontespa.myapplication.viewmodel.administrador

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.viewmodel.ReposP1Falsos
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
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
class RegistrarVehiculoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: RegistrarVehiculoViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = RegistrarVehiculoViewModel(aplicacionFalsa(), repos.flota)
        vm.escribirPatente(" ab-cd 12 "); vm.escribirCapacidad("20")
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun rechazaFormatoYCapacidadSinEnviarYConservaLosDatos() = runTest(dispatcher) {
        for (capacidad in listOf("", "abc", "2147483648", "0", "21")) {
            vm.escribirPatente("ABC123"); vm.escribirCapacidad(capacidad)
            vm.guardar(); runCurrent()
            assertEquals(setOf("patente", "capacidad"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
            assertEquals(capacidad, vm.estado.value.capacidad); assertTrue(vm.estado.value.modificado)
        }
        assertEquals(0, repos.enviosMovil)
    }

    @Test fun unGpsInvalidoNoSeConvierteEnAusente() = runTest(dispatcher) {
        for (gps in listOf("abc", "0", "-1", "1.5", "2147483648", "99999999999999999999999")) {
            vm.escribirGps(gps); vm.guardar(); runCurrent()
            assertEquals(setOf("gps"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        }
        assertEquals(0, repos.enviosMovil)
    }

    @Test fun normalizaLaPatenteAceptaGpsVacioYBloqueaElDobleEnvio() = runTest(dispatcher) {
        repos.espera = CompletableDeferred()
        vm.guardar(); vm.guardar(); runCurrent()
        assertEquals(1, repos.enviosMovil); assertTrue(vm.estado.value.enviando)
        assertEquals(NuevoMovil("ABCD12", 20, null), repos.nuevoMovil)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("ABCD12", vm.estado.value.registrado); assertFalse(vm.estado.value.modificado)
        vm.consumirRegistrado(); assertNull(vm.estado.value.registrado)
    }

    @Test fun unConflictoConservaElFormularioYPermiteCorregirLaPatente() = runTest(dispatcher) {
        vm.escribirGps("2147483647")
        repos.fallo = ErrorApp.Conflicto("La patente ya está registrada.")
        vm.guardar(); runCurrent()
        assertEquals(repos.fallo, vm.estado.value.error); assertNull(vm.estado.value.registrado)
        assertEquals("2147483647", vm.estado.value.gps); assertTrue(vm.estado.value.modificado)
        repos.fallo = null; vm.escribirPatente("zz-1234"); vm.escribirCapacidad("1")
        vm.guardar(); runCurrent()
        assertEquals(NuevoMovil("ZZ1234", 1, 2147483647L), repos.nuevoMovil)
        assertEquals("ZZ1234", vm.estado.value.registrado)
    }

    @Test fun elDetalleYLaListaLeenElVehiculoRegistradoConSuCodigo() = runTest(dispatcher) {
        vm.guardar(); runCurrent()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        val detalle = DetalleVehiculoViewModel(aplicacionFalsa(), repos.flota, repos.cuentas)
        detalle.cargar("ABCD12"); runCurrent()
        assertEquals("0123456789ABCDEF", detalle.estado.value.movil?.codigoAbordaje)
        assertEquals(EstadoMovil.DISPONIBLE, detalle.estado.value.movil?.estado)
        val lista = VehiculosViewModel(aplicacionFalsa(), repos.flota)
        lista.cargar(); runCurrent()
        assertEquals(listOf("ABCD12"), lista.estado.value.moviles.map { it.patente })
    }
}
