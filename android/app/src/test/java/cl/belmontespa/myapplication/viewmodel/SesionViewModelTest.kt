package cl.belmontespa.myapplication.viewmodel

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.repository.AuthRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sesiones = MutableSharedFlow<EstadoSesion>(replay = 1)

    private val auth = object : AuthRepository(soporteVacio()) {
        override fun observarSesion(): Flow<EstadoSesion> = sesiones
    }

    @Before fun preparar() { Dispatchers.setMain(dispatcher) }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun reloj() = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(dispatcher.scheduler.currentTime)
    }

    private fun sesion(expiraEn: Long) = Sesion("token", "usuario@belmontespa.cl", Rol.SOLICITANTE, expiraEn)

    @Test fun empiezaSinEstadoMientrasLeeLaSesionGuardada() = runTest(dispatcher) {
        val viewModel = SesionViewModel(aplicacionFalsa(), auth, reloj())
        runCurrent()
        assertNull(viewModel.estado.value)
    }

    @Test fun sigueElEstadoDeLaSesionYLoPasaASinSesionAlVencer() = runTest(dispatcher) {
        val viewModel = SesionViewModel(aplicacionFalsa(), auth, reloj())
        val autenticada = EstadoSesion.Autenticada(sesion(expiraEn = 1_000))
        sesiones.emit(autenticada)
        runCurrent()
        assertEquals(autenticada, viewModel.estado.value)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(autenticada, viewModel.estado.value)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
    }

    @Test fun unaSesionNuevaReemplazaLaEsperaDeLaAnterior() = runTest(dispatcher) {
        val viewModel = SesionViewModel(aplicacionFalsa(), auth, reloj())
        sesiones.emit(EstadoSesion.Autenticada(sesion(expiraEn = 1_000)))
        runCurrent()
        val nueva = EstadoSesion.Temporal(sesion(expiraEn = 5_000))
        sesiones.emit(nueva)
        runCurrent()

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(nueva, viewModel.estado.value)

        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
    }
}
