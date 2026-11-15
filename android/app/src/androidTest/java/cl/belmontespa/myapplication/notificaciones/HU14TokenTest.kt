package cl.belmontespa.myapplication.notificaciones

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.AuthRepository
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.api.ApiClient
import cl.belmontespa.myapplication.repository.dto.LoginRequest
import cl.belmontespa.myapplication.util.Reloj
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HU14TokenTest {
    @get:Rule val entorno = EntornoPruebas()
    private val servidor = MockWebServer()
    private val contexto: Context get() = ApplicationProvider.getApplicationContext()
    private val archivo = "hu14-prueba-${UUID.randomUUID()}"
    private lateinit var preferencias: PreferenciasNotificaciones
    private lateinit var auth: AuthRepository

    @Before fun preparar() {
        servidor.start()
        preferencias = PreferenciasNotificaciones(contexto, archivo)
        auth = AuthRepository(SoporteLocal(entorno.db, entorno.sesiones, Reloj.santiago,
            modoLocal = false, tokenDispositivo = preferencias))
        ApiClient.reemplazarParaPruebas(entorno.sesiones,
            ApiClient.crear(entorno.sesiones, servidor.url("/").toString()))
    }

    @After fun cerrar() {
        ApiClient.reemplazarParaPruebas(entorno.sesiones, null)
        servidor.shutdown()
        contexto.deleteSharedPreferences(archivo)
    }

    private fun responderLogin(correo: String = "ana@test.cl") {
        servidor.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
            {"accessToken":"jwt-prueba","tokenType":"Bearer","expiresIn":28800,
             "correo":"$correo","rol":"SOLICITANTE","cambioPasswordRequerido":false}
        """.trimIndent()))
    }

    private fun leerPedido() = checkNotNull(servidor.takeRequest(5, TimeUnit.SECONDS)) {
        "El servidor simulado no recibió la petición dentro de 5 segundos"
    }

    private fun leerLogin() = leerPedido().let { pedido ->
        assertEquals("/api/v1/auth/login", pedido.path)
        assertNull(pedido.getHeader("Authorization"))
        ApiClient.json.decodeFromString(LoginRequest.serializer(), pedido.body.readUtf8())
    }

    @Test fun elLoginIncluyeElTokenYElLogoutLoConservaParaLaSiguienteCuenta() = runBlocking {
        preferencias.guardarToken("token-dispositivo")
        responderLogin()
        assertTrue(auth.iniciarSesion("ana@test.cl", "Clave12345") is Resultado.Exito)
        assertEquals("token-dispositivo", leerLogin().tokenFcm)

        servidor.enqueue(MockResponse().setResponseCode(204))
        auth.cerrarSesion()
        val salida = leerPedido()
        assertEquals("/api/v1/auth/logout", salida.path)
        assertEquals("Bearer jwt-prueba", salida.getHeader("Authorization"))
        assertEquals("token-dispositivo", PreferenciasNotificaciones(contexto, archivo).leerToken())

        responderLogin("beto@test.cl")
        assertTrue(auth.iniciarSesion("beto@test.cl", "Clave12345") is Resultado.Exito)
        val segundoLogin = leerLogin()
        assertEquals("beto@test.cl", segundoLogin.correo)
        assertEquals("token-dispositivo", segundoLogin.tokenFcm)
    }

    @Test fun elTokenRenovadoSeEnviaPorTokenFcmConLaSesionActual() = runBlocking {
        responderLogin()
        assertTrue(auth.iniciarSesion("ana@test.cl", "Clave12345") is Resultado.Exito)
        assertNull(leerLogin().tokenFcm)
        preferencias.guardarToken("token-renovado")
        servidor.enqueue(MockResponse().setResponseCode(200))

        val resultado = registrarTokenFcm(entorno.sesiones.estado.first(),
            PreferenciasNotificaciones(contexto, archivo).leerToken(), auth::actualizarTokenFcm)

        assertEquals(ResultadoRegistroToken.LISTO, resultado)
        val pedido = leerPedido()
        assertEquals("POST", pedido.method)
        assertEquals("/api/v1/auth/token-fcm", pedido.path)
        assertEquals("Bearer jwt-prueba", pedido.getHeader("Authorization"))
        assertEquals("""{"tokenFcm":"token-renovado"}""", pedido.body.readUtf8())
    }

    @Test fun recuerdaElPermisoSolicitadoAlRecrearLasPreferencias() {
        assertFalse(preferencias.permisoSolicitado)
        preferencias.marcarPermisoSolicitado()
        assertTrue(PreferenciasNotificaciones(contexto, archivo).permisoSolicitado)
    }
}
