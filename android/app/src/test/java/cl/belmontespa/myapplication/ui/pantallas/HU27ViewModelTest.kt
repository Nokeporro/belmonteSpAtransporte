package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HU27ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    private fun cuenta(correo: String) = Solicitante("Persona Pendiente", correo, null, LocalDateTime.of(2026, 10, 24, 12, 0),
        "Empresa Original", "12345678-5", EstadoSolicitante.PENDIENTE)
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher); repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        repos.pendientesRevision = listOf(cuenta("a@test.cl"), cuenta("b@test.cl"))
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, Clock.systemUTC()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private fun cargar() { vm.cargar(PantallaDemo.CUENTAS_PENDIENTES, "") }
    @Test fun cargaLasCuentasPendientesConSuEmpresa() = runTest(dispatcher) {
        cargar(); runCurrent()
        assertEquals(repos.pendientesRevision, vm.estado.value.pendientes); assertNull(vm.estado.value.error)
        assertEquals("Empresa Original", vm.estado.value.pendientes.first().razonSocial)
    }
    @Test fun dobleToqueSoloRevisaUnaVezYQuitaLaCuentaConfirmada() = runTest(dispatcher) {
        cargar(); runCurrent(); repos.espera = CompletableDeferred()
        vm.ejecutar("aprobar", "a@test.cl"); vm.ejecutar("rechazarCuenta", "a@test.cl"); runCurrent()
        assertEquals(1, repos.revisionesCuenta); assertTrue(vm.estado.value.enviando)
        assertEquals(2, vm.estado.value.pendientes.size)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("a@test.cl" to true, repos.revisionCuenta)
        assertEquals(listOf("b@test.cl"), vm.estado.value.pendientes.map { it.correo })
        assertEquals(R.string.cuenta_revision_aprobada, vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
        vm.ejecutar("aprobar", "a@test.cl"); runCurrent(); assertEquals(1, repos.revisionesCuenta)
    }
    @Test fun rechazarQuitaLaFilaYConfirmaLaDecision() = runTest(dispatcher) {
        cargar(); runCurrent(); vm.ejecutar("rechazarCuenta", "b@test.cl"); runCurrent()
        assertEquals("b@test.cl" to false, repos.revisionCuenta)
        assertEquals(R.string.cuenta_revision_rechazada, vm.estado.value.aviso)
        assertEquals(listOf("a@test.cl"), vm.estado.value.pendientes.map { it.correo })
    }
    @Test fun sinConexionConservaLaFilaYPermiteReintentar() = runTest(dispatcher) {
        cargar(); runCurrent(); repos.fallo = ErrorApp.SinConexion
        vm.ejecutar("aprobar", "a@test.cl"); runCurrent()
        assertEquals(repos.pendientesRevision, vm.estado.value.pendientes)
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertNull(vm.estado.value.aviso)
        repos.fallo = null; vm.actualizar(); runCurrent(); vm.ejecutar("aprobar", "a@test.cl"); runCurrent()
        assertEquals(R.string.cuenta_revision_aprobada, vm.estado.value.aviso)
    }
    @Test fun conflictoRecargaLasCuentasSinAfirmarQueLaDecisionSeGuardo() = runTest(dispatcher) {
        cargar(); runCurrent(); repos.fallo = ErrorApp.Conflicto("La cuenta ya fue revisada.")
        repos.pendientesRevision = listOf(cuenta("b@test.cl"))
        vm.ejecutar("aprobar", "a@test.cl"); runCurrent()
        assertEquals(repos.pendientesRevision, vm.estado.value.pendientes)
        assertEquals(repos.fallo, vm.estado.value.error); assertNull(vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
    }
    @Test fun falloAlListarSeMuestraYReintentarRecuperaLaLista() = runTest(dispatcher) {
        repos.errorPendientes = ErrorApp.SinConexion; cargar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertFalse(vm.estado.value.cargando)
        repos.errorPendientes = null; vm.actualizar(); runCurrent()
        assertEquals(2, vm.estado.value.pendientes.size); assertNull(vm.estado.value.error)
    }
    @Test fun unSolicitanteNoPuedeRevisarNiAunqueInvoqueLaAccion() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.SOLICITANTE); cargar(); runCurrent()
        vm.ejecutar("aprobar", "a@test.cl"); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.revisionesCuenta)
    }
}
