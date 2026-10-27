package cl.belmontespa.myapplication.viewmodel.administrador

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.viewmodel.ReposP1Falsos
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import java.time.LocalDateTime
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
class CuentasPendientesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: CuentasPendientesViewModel

    private fun cuenta(correo: String) = Solicitante("Persona Pendiente", correo, null, LocalDateTime.of(2026, 10, 24, 12, 0),
        "Empresa Original", "12345678-5", EstadoSolicitante.PENDIENTE)

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        repos.pendientesRevision = listOf(cuenta("a@test.cl"), cuenta("b@test.cl"))
        vm = CuentasPendientesViewModel(aplicacionFalsa(), repos.cuentas)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun cargaLasCuentasPendientesConSuEmpresa() = runTest(dispatcher) {
        vm.cargar(); runCurrent()
        assertEquals(repos.pendientesRevision, vm.estado.value.pendientes); assertNull(vm.estado.value.error)
        assertEquals("Empresa Original", vm.estado.value.pendientes.first().razonSocial)
    }

    @Test fun dobleToqueSoloRevisaUnaVezYQuitaLaCuentaConfirmada() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); repos.espera = CompletableDeferred()
        vm.aprobar("a@test.cl"); vm.rechazar("a@test.cl"); runCurrent()
        assertEquals(1, repos.revisionesCuenta); assertTrue(vm.estado.value.enviando)
        assertEquals(2, vm.estado.value.pendientes.size)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("a@test.cl" to true, repos.revisionCuenta)
        assertEquals(listOf("b@test.cl"), vm.estado.value.pendientes.map { it.correo })
        assertEquals(R.string.cuenta_revision_aprobada, vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
        vm.aprobar("a@test.cl"); runCurrent(); assertEquals(1, repos.revisionesCuenta)
    }

    @Test fun rechazarQuitaLaFilaYConfirmaLaDecision() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); vm.rechazar("b@test.cl"); runCurrent()
        assertEquals("b@test.cl" to false, repos.revisionCuenta)
        assertEquals(R.string.cuenta_revision_rechazada, vm.estado.value.aviso)
        assertEquals(listOf("a@test.cl"), vm.estado.value.pendientes.map { it.correo })
    }

    @Test fun sinConexionConservaLaFilaYPermiteReintentar() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); repos.fallo = ErrorApp.SinConexion
        vm.aprobar("a@test.cl"); runCurrent()
        assertEquals(repos.pendientesRevision, vm.estado.value.pendientes)
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertNull(vm.estado.value.aviso)
        repos.fallo = null; vm.cargar(); runCurrent(); vm.aprobar("a@test.cl"); runCurrent()
        assertEquals(R.string.cuenta_revision_aprobada, vm.estado.value.aviso)
    }

    @Test fun unConflictoRecargaLasCuentasSinAfirmarQueLaDecisionSeGuardo() = runTest(dispatcher) {
        vm.cargar(); runCurrent(); repos.fallo = ErrorApp.Conflicto("La cuenta ya fue revisada.")
        repos.pendientesRevision = listOf(cuenta("b@test.cl"))
        vm.aprobar("a@test.cl"); runCurrent()
        assertEquals(repos.pendientesRevision, vm.estado.value.pendientes)
        assertEquals(repos.fallo, vm.estado.value.error); assertNull(vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
    }

    @Test fun elFalloAlListarSeMuestraYReintentarRecuperaLaLista() = runTest(dispatcher) {
        repos.errorPendientes = ErrorApp.SinConexion; vm.cargar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error); assertFalse(vm.estado.value.cargando)
        repos.errorPendientes = null; vm.cargar(); runCurrent()
        assertEquals(2, vm.estado.value.pendientes.size); assertNull(vm.estado.value.error)
    }

    @Test fun unSolicitanteNoPuedeRevisarNiAunqueInvoqueLaAccion() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.SOLICITANTE); vm.cargar(); runCurrent()
        vm.aprobar("a@test.cl"); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.revisionesCuenta)
    }
}
