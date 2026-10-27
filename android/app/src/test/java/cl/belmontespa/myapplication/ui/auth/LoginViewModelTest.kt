package cl.belmontespa.myapplication.ui.auth

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.MotivoSinPermiso
import cl.belmontespa.myapplication.ui.ReposP1Falsos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repos: ReposP1Falsos

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    @Test fun elLoginConservaElErrorGenericoYDistingueLaCuentaPendiente() = runTest(dispatcher) {
        val login = LoginViewModel(repos)
        login.cambiarCorreo("cuenta@test.cl"); login.cambiarPassword("Clave12345")
        repos.fallo = ErrorApp.SesionExpirada; login.iniciarSesion(); runCurrent()
        assertEquals(R.string.error_credenciales, login.estado.value.error)
        repos.fallo = ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_PENDIENTE); login.iniciarSesion(); runCurrent()
        assertEquals(R.string.error_cuenta_pendiente, login.estado.value.error)
        repos.fallo = null; login.iniciarSesion(); runCurrent()
        assertEquals("", login.estado.value.password)
    }
}
