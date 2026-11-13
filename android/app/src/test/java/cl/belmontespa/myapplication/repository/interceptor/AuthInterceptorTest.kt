package cl.belmontespa.myapplication.repository.interceptor

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.repository.api.ApiClient
import cl.belmontespa.myapplication.repository.dto.AbordajeRequest
import cl.belmontespa.myapplication.repository.dto.LoginRequest
import cl.belmontespa.myapplication.repository.dto.RecuperarPasswordRequest
import cl.belmontespa.myapplication.repository.dto.SolicitanteRequest
import cl.belmontespa.myapplication.repository.sesionDePrueba
import cl.belmontespa.myapplication.repository.sesionesEnMemoria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class AuthInterceptorTest {

    private val servidor = MockWebServer()
    private val sesiones = sesionesEnMemoria()

    private val usuario = """{"nombre":"Ana","correo":"ana@belmontespa.cl","telefono":null,"fechaRegistro":"2026-11-01T10:00:00"}"""
    private val login = """{"accessToken":"nuevo","tokenType":"Bearer","expiresIn":60,"correo":"ana@belmontespa.cl","rol":"ADMINISTRADOR","cambioPasswordRequerido":false}"""

    @Before
    fun abrir() = servidor.start()

    @After
    fun cerrar() = servidor.shutdown()

    private fun solicitante() = SolicitanteRequest("Sara", "s@x.cl", null, "Clave1234", "Acme", "76543210-3")

    private fun api() = ApiClient.crear(sesiones, servidor.url("/").toString())

    @Test
    fun unaRutaPrivadaLlevaElTokenDeLaSesion() = runBlocking {
        sesiones.guardar(sesionDePrueba("jwt-1"))
        servidor.enqueue(MockResponse().setBody(usuario))

        api().usuario("ana@belmontespa.cl")

        assertEquals("Bearer jwt-1", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun laSesionTemporalTambienLlevaSuToken() = runBlocking {
        sesiones.guardarTemporal(sesionDePrueba("jwt-temporal"))
        servidor.enqueue(MockResponse().setBody(usuario))

        api().usuario("ana@belmontespa.cl")

        assertEquals("Bearer jwt-temporal", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun elLoginElRegistroYElAbordajeVanSinToken() = runBlocking {
        sesiones.guardar(sesionDePrueba("jwt-1"))
        servidor.enqueue(MockResponse().setBody(login))
        servidor.enqueue(MockResponse().setResponseCode(404))
        servidor.enqueue(MockResponse().setResponseCode(404))

        api().login(LoginRequest("ana@belmontespa.cl", "Clave1234"))
        runCatching { api().abordar(AbordajeRequest("KXJR42", "7F3A9C21B04E58D6", "RUT", "15234567-4")) }
        runCatching { api().registrarSolicitante(solicitante()) }

        repeat(3) { assertNull(servidor.takeRequest().getHeader("Authorization")) }
    }

    @Test
    fun laRecuperacionDeContrasenaSiLlevaElTokenDelAdministrador() = runBlocking {
        sesiones.guardar(sesionDePrueba("jwt-admin"))
        servidor.enqueue(MockResponse().setResponseCode(404))

        runCatching { api().recuperarPassword(RecuperarPasswordRequest("x@y.cl")) }

        assertEquals("Bearer jwt-admin", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun unaRespuesta401ConTokenBorraLaSesion() = runBlocking {
        sesiones.guardar(sesionDePrueba())
        servidor.enqueue(MockResponse().setResponseCode(401))

        val fallo = runCatching { api().usuario("ana@belmontespa.cl") }.exceptionOrNull()

        assertTrue(fallo is HttpException && fallo.code() == 401)
        assertEquals(EstadoSesion.SinSesion, sesiones.estado.first())
    }

    @Test
    fun unLoginRechazadoNoTocaLaSesionGuardada() = runBlocking {
        sesiones.guardar(sesionDePrueba("jwt-1"))
        servidor.enqueue(MockResponse().setResponseCode(401))

        runCatching { api().login(LoginRequest("ana@belmontespa.cl", "mala")) }

        assertEquals(EstadoSesion.Autenticada(sesionDePrueba("jwt-1")), sesiones.estado.first())
    }
}
