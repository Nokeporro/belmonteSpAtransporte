package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
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
class DetalleConductorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: DetalleConductorViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        repos.perfil = repos.perfil.copy(rol = Rol.ADMINISTRADOR)
        vm = DetalleConductorViewModel(repos, repos)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun editarSinClaveConservaLaActualYActualizaElDetalle() = runTest(dispatcher) {
        vm.cargar(repos.conductor.correo); runCurrent()
        vm.escribirNombre("  Nombre Editado  ")
        vm.guardar(); runCurrent()
        assertNull(repos.passwordConductor)
        assertEquals("Nombre Editado", vm.estado.value.conductor?.nombre)
        assertEquals("Nombre Editado", vm.estado.value.nombre)
        assertFalse(vm.estado.value.modificado)
    }

    @Test fun elFalloDeEdicionYLaRecargaNoPierdenLosDatosDelAdministrador() = runTest(dispatcher) {
        vm.cargar(repos.conductor.correo); runCurrent()
        vm.escribirNombre("Nombre sin guardar"); vm.escribirPassword("NuevaClave123")
        repos.fallo = ErrorApp.SinConexion
        vm.guardar(); runCurrent(); vm.reintentar(); runCurrent()
        assertEquals("Nombre sin guardar", vm.estado.value.nombre); assertEquals("NuevaClave123", vm.estado.value.password)
        assertTrue(vm.estado.value.modificado)
        repos.fallo = null; vm.guardar(); runCurrent()
        assertEquals("NuevaClave123", repos.passwordConductor)
        assertEquals("", vm.estado.value.password)
    }

    @Test fun desactivaYReactivaConRespuestaConfirmadaSinPerderLaEdicion() = runTest(dispatcher) {
        vm.cargar(repos.conductor.correo); runCurrent()
        vm.escribirNombre("Edición pendiente")
        repos.espera = CompletableDeferred()
        vm.cambiarActivo(); vm.cambiarActivo(); runCurrent()
        assertTrue(vm.estado.value.conductor!!.activo); assertEquals(1, repos.cambiosActivo)
        repos.espera!!.complete(Unit); runCurrent()
        assertFalse(vm.estado.value.conductor!!.activo); assertEquals("Edición pendiente", vm.estado.value.nombre)
        assertEquals(R.string.flota_activo_guardado, vm.estado.value.aviso)
        vm.cambiarActivo(); runCurrent(); assertTrue(vm.estado.value.conductor!!.activo)
    }

    @Test fun soloElAdministradorPuedeCambiarElActivo() = runTest(dispatcher) {
        repos.perfil = repos.perfil.copy(rol = Rol.CONDUCTOR)
        vm.cargar(repos.conductor.correo); runCurrent()
        vm.cambiarActivo(); runCurrent()
        assertTrue(vm.estado.value.error is ErrorApp.SinPermiso); assertEquals(0, repos.cambiosActivo)
    }

    @Test fun elConflictoNoAfirmaExitoNiCambiaElEstado() = runTest(dispatcher) {
        vm.cargar(repos.conductor.correo); runCurrent()
        repos.fallo = ErrorApp.Conflicto("Tiene viajes asignados.")
        vm.cambiarActivo(); runCurrent()
        assertTrue(vm.estado.value.conductor!!.activo); assertEquals(repos.fallo, vm.estado.value.error)
        assertNull(vm.estado.value.aviso); assertFalse(vm.estado.value.enviando)
    }
}
