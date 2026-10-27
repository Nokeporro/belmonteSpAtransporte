package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.Rol
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
class DetalleVehiculoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: DetalleVehiculoViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        repos.moviles.add(Movil("ZZ1234", 4, null, true, EstadoMovil.DISPONIBLE, "0123456789ABCDEF"))
        vm = DetalleVehiculoViewModel(repos, repos)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun elMovilConservaQrCapacidadYGps() = runTest(dispatcher) {
        vm.cargar("ZZ1234"); runCurrent()
        val original = vm.estado.value.movil!!
        vm.cambiarActivo(); runCurrent(); assertEquals(original.copy(activo = false), vm.estado.value.movil)
        vm.cambiarActivo(); runCurrent(); assertEquals(original, vm.estado.value.movil)
    }

    @Test fun elConflictoYLaDesconexionNoAfirmanExitoNiCambianElEstado() = runTest(dispatcher) {
        vm.cargar("ZZ1234"); runCurrent()
        for (error in listOf(ErrorApp.Conflicto("Tiene viajes asignados."), ErrorApp.SinConexion)) {
            repos.fallo = error; vm.cambiarActivo(); runCurrent()
            assertTrue(vm.estado.value.movil!!.activo); assertEquals(error, vm.estado.value.error)
            assertNull(vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
        }
        repos.fallo = null; vm.reintentar(); runCurrent(); vm.cambiarActivo(); runCurrent()
        assertFalse(vm.estado.value.movil!!.activo)
    }

    @Test fun soloElAdministradorPuedeCambiarElActivo() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.CONDUCTOR)
        vm.cargar("ZZ1234"); runCurrent(); vm.cambiarActivo(); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.cambiosActivo)
    }

    @Test fun unVehiculoInexistenteSeInformaComoNoEncontrado() = runTest(dispatcher) {
        vm.cargar("NOEXISTE"); runCurrent()
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error); assertNull(vm.estado.value.movil)
    }
}
