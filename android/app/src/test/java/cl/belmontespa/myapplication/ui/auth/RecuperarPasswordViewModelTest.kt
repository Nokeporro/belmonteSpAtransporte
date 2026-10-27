package cl.belmontespa.myapplication.ui.auth

import cl.belmontespa.myapplication.R
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecuperarPasswordViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: RecuperarPasswordViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = RecuperarPasswordViewModel(repos)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun correoInvalidoNoSeEnvia() = runTest(dispatcher) {
        for (correo in listOf("", "no-es-correo", "a b@test.cl", "a".repeat(145) + "@test.cl")) {
            vm.escribirCorreo(correo); vm.recuperar(); runCurrent()
            assertEquals(setOf("correo"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        }
        assertEquals(0, repos.recuperaciones)
    }

    @Test fun normalizaElCorreoYBloqueaElDobleToque() = runTest(dispatcher) {
        vm.escribirCorreo("  ANA@Test.CL  ")
        repos.espera = CompletableDeferred()
        vm.recuperar(); vm.recuperar(); runCurrent()
        assertEquals(1, repos.recuperaciones); assertEquals("ana@test.cl", repos.correoRecuperacion)
        assertTrue(vm.estado.value.enviando); assertNull(vm.estado.value.aviso)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(R.string.aviso_recuperacion_enviada, vm.estado.value.aviso)
        assertTrue(vm.estado.value.enviado); assertFalse(vm.estado.value.modificado)
    }

    @Test fun sinConexionConservaElCorreoYNoMuestraLaConfirmacion() = runTest(dispatcher) {
        vm.escribirCorreo("ana@test.cl")
        repos.fallo = ErrorApp.SinConexion; vm.recuperar(); runCurrent()
        assertEquals("ana@test.cl", vm.estado.value.correo); assertEquals(repos.fallo, vm.estado.value.error)
        assertNull(vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
        repos.fallo = null; vm.recuperar(); runCurrent()
        assertEquals(R.string.aviso_recuperacion_enviada, vm.estado.value.aviso)
    }
}
