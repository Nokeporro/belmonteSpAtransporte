package cl.belmontespa.myapplication.ui.solicitante

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.TipoDocumento
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
class AgregarPasajeroViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: AgregarPasajeroViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = AgregarPasajeroViewModel(repos)
        vm.escribirNombre("Invitado"); vm.elegirTipoDocumento("PASAPORTE"); vm.escribirDocumento("DEMO123")
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun dobleToqueAgregaUnaVezYVuelveAlDetalle() = runTest(dispatcher) {
        repos.espera = CompletableDeferred()
        vm.agregar("TR-1051"); vm.agregar("TR-1051"); runCurrent()
        assertEquals(1, repos.enviosPasajero); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("TR-1051" to NuevoPasajero(TipoDocumento.PASAPORTE, "DEMO123", "Invitado"), repos.nuevoPasajero)
        assertTrue(vm.estado.value.agregado); assertFalse(vm.estado.value.enviando); assertFalse(vm.estado.value.modificado)
    }

    @Test fun elFalloConservaLosDatosYPermiteReintentar() = runTest(dispatcher) {
        repos.fallo = ErrorApp.Validacion(mapOf("documento" to "Documento inválido."))
        vm.agregar("TR-1051"); runCurrent()
        assertEquals(setOf("documento"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals("DEMO123", vm.estado.value.documento); assertTrue(vm.estado.value.modificado)
        assertFalse(vm.estado.value.agregado)
        repos.fallo = null; vm.agregar("TR-1051"); runCurrent()
        assertTrue(vm.estado.value.agregado); assertNull(vm.estado.value.error)
    }
}
