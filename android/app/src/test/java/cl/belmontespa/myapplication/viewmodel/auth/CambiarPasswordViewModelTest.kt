package cl.belmontespa.myapplication.viewmodel.auth

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CambiarPasswordViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: CambiarPasswordViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = CambiarPasswordViewModel(aplicacionFalsa(), repos.cuentas)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun datos(actual: String = "Temporal1234", nueva: String = "NuevaClave123", confirmacion: String = nueva) {
        vm.escribirActual(actual); vm.escribirNueva(nueva); vm.escribirConfirmacion(confirmacion)
    }

    @Test fun cargaLaCuentaDeLaSesion() = runTest(dispatcher) {
        vm.cargar(); runCurrent()
        assertEquals(repos.perfil, vm.estado.value.cuenta); assertTrue(!vm.estado.value.cargando)
    }

    @Test fun validaLosCamposIncluidoElLimiteDeBytes() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); datos("", "soloLetras", "distinta")
        vm.guardar(); runCurrent()
        assertEquals(
            setOf("passwordActual", "passwordNueva", "confirmacion"),
            (vm.estado.value.error as ErrorApp.Validacion).errores.keys,
        )
        datos(nueva = "á".repeat(36) + "1"); vm.guardar(); runCurrent()
        assertEquals(setOf("passwordNueva"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.cambiosPassword)
    }

    @Test fun elErrorDeLaClaveActualConservaLosCamposYElExitoLosBorraSinRepetirElEnvio() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); datos()
        repos.fallo = ErrorApp.Validacion(mapOf("passwordActual" to "No coincide."))
        vm.guardar(); runCurrent()
        assertEquals("Temporal1234", vm.estado.value.passwordActual)
        assertEquals("NuevaClave123", vm.estado.value.passwordNueva); assertNull(vm.estado.value.aviso)
        repos.fallo = null; repos.espera = CompletableDeferred()
        vm.guardar(); vm.guardar(); runCurrent()
        assertEquals(2, repos.cambiosPassword)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("", vm.estado.value.passwordActual)
        assertEquals("", vm.estado.value.passwordNueva); assertEquals("", vm.estado.value.confirmacion)
        assertEquals("Temporal1234" to "NuevaClave123", repos.clavesCambiadas)
        assertEquals(R.string.aviso_password_guardada, vm.estado.value.aviso)
    }

    @Test fun elConductorNoPuedeCambiarSuClave() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.CONDUCTOR)
        vm.cargar(); runCurrent(); datos(); vm.guardar(); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.cambiosPassword)
    }
}
