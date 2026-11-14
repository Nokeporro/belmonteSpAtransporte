package cl.belmontespa.myapplication.viewmodel.auth

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import cl.belmontespa.myapplication.viewmodel.soporteVacio
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    private val viajes = ColaFalsa()

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear() = PerfilViewModel(aplicacionFalsa(), CuentaRepository(soporteVacio()), viajes)

    @Test
    fun sinCambiosPendientesCierraLaSesionDeInmediato() {
        val vm = crear()

        vm.pedirCerrarSesion()

        assertTrue(vm.estado.value.cerrar)
        assertFalse(vm.estado.value.confirmandoCierre)
    }

    @Test
    fun conCambiosPendientesPideSincronizarODescartar() {
        viajes.pendientes = 2
        val vm = crear()

        vm.pedirCerrarSesion()

        assertTrue(vm.estado.value.confirmandoCierre)
        assertFalse(vm.estado.value.cerrar)
    }

    @Test
    fun sincronizarCierraLaSesionCuandoSeEnviaronTodos() {
        viajes.pendientes = 1
        val vm = crear()
        vm.pedirCerrarSesion()

        vm.sincronizarYCerrar()

        assertEquals(1, viajes.sincronizaciones)
        assertTrue(vm.estado.value.cerrar)
        assertFalse(vm.estado.value.confirmandoCierre)
        assertFalse(vm.estado.value.sincronizando)
    }

    @Test
    fun siLaSincronizacionFallaSeQuedaEnElDialogoConElError() {
        viajes.pendientes = 1
        viajes.resultadoSincronizacion = Resultado.Fallo(ErrorApp.SinConexion)
        val vm = crear()
        vm.pedirCerrarSesion()

        vm.sincronizarYCerrar()

        assertFalse(vm.estado.value.cerrar)
        assertTrue(vm.estado.value.confirmandoCierre)
        assertFalse(vm.estado.value.sincronizando)
        assertEquals(ErrorApp.SinConexion, vm.estado.value.errorSincronizacion)
    }

    @Test
    fun descartarCierraLaSesionSinSincronizar() {
        viajes.pendientes = 1
        val vm = crear()
        vm.pedirCerrarSesion()

        vm.descartarYCerrar()

        assertTrue(vm.estado.value.cerrar)
        assertFalse(vm.estado.value.confirmandoCierre)
        assertEquals(0, viajes.sincronizaciones)
    }

    @Test
    fun cancelarVuelveAlPerfilSinCerrar() {
        viajes.pendientes = 1
        viajes.resultadoSincronizacion = Resultado.Fallo(ErrorApp.SinConexion)
        val vm = crear()
        vm.pedirCerrarSesion()
        vm.sincronizarYCerrar()

        vm.cancelarCierre()

        assertFalse(vm.estado.value.confirmandoCierre)
        assertFalse(vm.estado.value.cerrar)
        assertNull(vm.estado.value.errorSincronizacion)
    }

    @Test
    fun consumirElCierreLimpiaLaOrden() {
        val vm = crear()
        vm.pedirCerrarSesion()

        vm.consumirCierre()

        assertFalse(vm.estado.value.cerrar)
    }

    @Test
    fun unFalloAlContarNoImpideCerrarSesion() {
        viajes.fallaAlContar = true
        val vm = crear()

        vm.pedirCerrarSesion()

        assertTrue(vm.estado.value.cerrar)
    }

    @Test
    fun mientrasSincronizaNoAceptaOtraAccion() {
        viajes.pendientes = 1
        val espera = CompletableDeferred<Unit>()
        viajes.espera = espera
        val vm = crear()
        vm.pedirCerrarSesion()

        vm.sincronizarYCerrar()
        assertTrue(vm.estado.value.sincronizando)
        vm.descartarYCerrar()
        vm.cancelarCierre()
        vm.sincronizarYCerrar()

        assertTrue(vm.estado.value.confirmandoCierre)
        assertFalse(vm.estado.value.cerrar)
        assertEquals(1, viajes.sincronizaciones)

        espera.complete(Unit)

        assertTrue(vm.estado.value.cerrar)
    }
}

private class ColaFalsa : ViajeRepository(soporteVacio()) {
    var pendientes = 0
    var fallaAlContar = false
    var sincronizaciones = 0
    var resultadoSincronizacion: Resultado<Unit> = Resultado.Exito(Unit)
    var espera: CompletableDeferred<Unit>? = null

    override suspend fun contarPendientes(): Int {
        if (fallaAlContar) throw IllegalStateException("sin acceso a los datos")
        return pendientes
    }

    override suspend fun sincronizarAhora(): Resultado<Unit> {
        sincronizaciones++
        espera?.await()
        return resultadoSincronizacion
    }
}
