package cl.belmontespa.myapplication.viewmodel.auth

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSolicitante
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: RegistroViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = RegistroViewModel(aplicacionFalsa(), repos.auth)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun datos() {
        vm.escribirNombre("Ana Pérez"); vm.escribirCorreo("ana@test.cl"); vm.escribirTelefono("")
        vm.escribirPassword("Clave12345"); vm.escribirRazonSocial("Empresa SpA"); vm.escribirRutEmpresa("76123456-0")
    }

    @Test fun escribirMarcaElFormularioComoModificado() = runTest(dispatcher) {
        assertFalse(vm.estado.value.modificado)
        vm.escribirNombre("Ana")
        assertTrue(vm.estado.value.modificado)
    }

    @Test fun dobleToqueEnviaUnaVezYUnaEmpresaNuevaQuedaPendiente() = runTest(dispatcher) {
        datos(); repos.espera = CompletableDeferred()
        vm.registrar(); vm.registrar(); runCurrent()
        assertEquals(1, repos.enviosRegistro); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(R.string.aviso_registro_pendiente, vm.estado.value.aviso)
        assertTrue(vm.estado.value.registrado); assertFalse(vm.estado.value.enviando)
        assertFalse(vm.estado.value.modificado)
        assertNull(repos.registro!!.telefono)
    }

    @Test fun unaEmpresaYaAprobadaQuedaAprobada() = runTest(dispatcher) {
        datos(); repos.estadoRegistro = EstadoSolicitante.APROBADO
        vm.registrar(); runCurrent()
        assertEquals(R.string.aviso_registro_aprobado, vm.estado.value.aviso)
    }

    @Test fun elFalloConservaLosDatosYPermiteReintentar() = runTest(dispatcher) {
        datos(); repos.fallo = ErrorApp.Conflicto("El correo ya está registrado.")
        vm.registrar(); runCurrent()
        assertEquals(repos.fallo, vm.estado.value.error); assertFalse(vm.estado.value.registrado)
        assertEquals("ana@test.cl", vm.estado.value.correo); assertEquals("Clave12345", vm.estado.value.password)
        assertTrue(vm.estado.value.modificado); assertFalse(vm.estado.value.enviando)
        repos.fallo = null; vm.registrar(); runCurrent()
        assertTrue(vm.estado.value.registrado); assertNull(vm.estado.value.error)
    }

    @Test fun losErroresPorCampoDelRepositorioLlegaranALaPantalla() = runTest(dispatcher) {
        datos(); repos.fallo = ErrorApp.Validacion(mapOf("correo" to "Ingresa un correo válido."))
        vm.registrar(); runCurrent()
        assertEquals(setOf("correo"), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        vm.escribirCorreo("otro@test.cl")
        assertNull(vm.estado.value.error)
    }
}
