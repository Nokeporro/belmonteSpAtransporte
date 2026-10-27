package cl.belmontespa.myapplication.viewmodel.administrador

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.repository.cuenta
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
class RegistrarAdminViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: RegistrarAdminViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        vm = RegistrarAdminViewModel(aplicacionFalsa(), repos.cuentas)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun formulario() {
        vm.escribirNombre("  Nueva Administradora  "); vm.escribirCorreo("  ADMIN@TEST.CL  ")
        vm.escribirTelefono("9 5555-0101"); vm.escribirPassword("Inicial12345")
    }

    @Test fun losErroresPorCampoNoEnvianElAlta() = runTest(dispatcher) {
        vm.cargar(); runCurrent()
        vm.escribirNombre("A"); vm.escribirCorreo("invalido"); vm.escribirTelefono("123"); vm.escribirPassword("abcdefgh")
        vm.guardar(); runCurrent()
        assertEquals(
            setOf("nombre", "correo", "telefono", "password"),
            (vm.estado.value.error as ErrorApp.Validacion).errores.keys,
        )
        assertEquals(0, repos.enviosAdministrador)
    }

    @Test fun dobleToqueSoloGuardaUnaVezYLimpiaLaClaveSinCambiarLaSesion() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); formulario(); repos.espera = CompletableDeferred()
        vm.guardar(); vm.guardar(); runCurrent()
        assertEquals(1, repos.enviosAdministrador); assertTrue(vm.estado.value.enviando)
        assertEquals("Inicial12345", vm.estado.value.password); assertNull(vm.estado.value.aviso)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("", vm.estado.value.password); assertEquals("admin@test.cl", vm.estado.value.correo)
        assertEquals("Nueva Administradora", vm.estado.value.nombre); assertEquals("+56955550101", vm.estado.value.telefono)
        assertEquals(R.string.admin_alta_guardada, vm.estado.value.aviso); assertFalse(vm.estado.value.modificado)
        assertEquals(repos.perfil, vm.estado.value.cuenta)
    }

    @Test fun elConflictoYLaDesconexionConservanLosDatosParaCorregirYReintentar() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); formulario()
        for (error in listOf(ErrorApp.Conflicto("El correo ya está registrado."), ErrorApp.SinConexion)) {
            repos.fallo = error; vm.guardar(); runCurrent()
            assertEquals(error, vm.estado.value.error); assertEquals("Inicial12345", vm.estado.value.password)
            assertNull(vm.estado.value.aviso); assertTrue(vm.estado.value.modificado)
        }
        repos.fallo = null; vm.escribirCorreo("otra@test.cl"); vm.guardar(); runCurrent()
        assertEquals("otra@test.cl", repos.cuentaAdministrador!!.correo); assertEquals("", vm.estado.value.password)
    }

    @Test fun otroRolNoPuedeCrearUnAdministrador() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.SOLICITANTE)
        vm.cargar(); runCurrent(); formulario(); vm.guardar(); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.enviosAdministrador)
    }
}
