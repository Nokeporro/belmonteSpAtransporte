package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.model.ErrorApp
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
class RegistrarConductorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: RegistrarConductorViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = RegistrarConductorViewModel(repos)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun formulario() {
        vm.escribirNombre("Ana Conductora"); vm.escribirCorreo("ana@test.cl")
        vm.escribirTelefono("9 5555-0101"); vm.escribirPassword("Clave12345")
    }

    @Test fun unaAltaInvalidaNoLlamaAlRepositorioYMuestraErroresPorCampo() = runTest(dispatcher) {
        vm.escribirNombre("A"); vm.escribirCorreo("invalido"); vm.escribirPassword("abcdefgh"); vm.escribirTelefono("123")
        vm.guardar(); runCurrent()
        assertEquals(
            setOf("nombre", "correo", "telefono", "password"),
            (vm.estado.value.error as ErrorApp.Validacion).errores.keys,
        )
        assertEquals(0, repos.enviosConductor); assertTrue(vm.estado.value.modificado)
    }

    @Test fun bloqueaElDobleEnvioYBorraLaClaveAntesDeNavegarAlDetalle() = runTest(dispatcher) {
        formulario(); repos.espera = CompletableDeferred()
        vm.guardar(); vm.guardar(); runCurrent()
        assertEquals(1, repos.enviosConductor); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(repos.conductor.correo, vm.estado.value.registrado)
        assertEquals("", vm.estado.value.password); assertFalse(vm.estado.value.modificado)
        vm.consumirRegistrado(); assertNull(vm.estado.value.registrado)
    }

    @Test fun unConflictoConservaLosDatosYPermiteCorregirElCorreo() = runTest(dispatcher) {
        formulario(); repos.fallo = ErrorApp.Conflicto("El correo ya está registrado.")
        vm.guardar(); runCurrent()
        assertEquals(repos.fallo, vm.estado.value.error)
        assertEquals("Clave12345", vm.estado.value.password); assertNull(vm.estado.value.registrado)
        vm.escribirCorreo("otra@test.cl"); repos.fallo = null
        vm.guardar(); runCurrent()
        assertEquals("otra@test.cl", repos.cuentaConductor?.correo)
        assertNotNull(vm.estado.value.registrado)
    }

    @Test fun unaClaveDeMasDe72BytesNoSeEnvia() = runTest(dispatcher) {
        formulario(); vm.escribirPassword("é".repeat(36) + "1")
        vm.guardar(); runCurrent()
        assertTrue("password" in (vm.estado.value.error as ErrorApp.Validacion).errores)
        assertEquals(0, repos.enviosConductor)
    }
}
