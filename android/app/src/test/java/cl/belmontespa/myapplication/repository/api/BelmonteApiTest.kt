package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.repository.dto.ActivoRequest
import cl.belmontespa.myapplication.repository.dto.CancelarRequest
import cl.belmontespa.myapplication.repository.dto.TokenFcmRequest
import cl.belmontespa.myapplication.repository.dto.UsuarioRequest
import cl.belmontespa.myapplication.repository.sesionDePrueba
import cl.belmontespa.myapplication.repository.sesionesEnMemoria
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BelmonteApiTest {

    private val servidor = MockWebServer()
    private val sesiones = sesionesEnMemoria()

    private val viaje = """
        {"folio":"TR-1042","estado":"CANCELADO","correoSolicitante":"s@x.cl","nombreSolicitante":"Sara","telefonoSolicitante":null,
         "razonSocial":"Acme","origen":{"direccion":"A","comuna":"B","region":"C","latitud":1.0,"longitud":2.0},
         "destino":{"direccion":"D","comuna":"E","region":"F","latitud":3.0,"longitud":4.0},
         "fhCreacion":"2026-11-10T09:00:00","fhAgenda":"2026-11-14T14:30:00","fhAsignacion":null,"fhInicio":null,"fhFin":null,
         "numeroVuelo":null,"cantidadPasajeros":0,"correoConductor":null,"nombreConductor":null,"patenteMovil":null,
         "hashPago":null,"estadoPago":null,"motivo":"Cambio de planes","pasajeros":[]}
    """.trimIndent()

    @Before
    fun abrir() {
        servidor.start()
        runBlocking { sesiones.guardar(sesionDePrueba()) }
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun api() = ApiClient.crear(sesiones, servidor.url("/").toString())

    @Test
    fun elSolicitanteCancelaSinCuerpo() = runBlocking {
        servidor.enqueue(MockResponse().setBody(viaje))

        api().cancelarComoSolicitante("TR-1042")

        val pedido = servidor.takeRequest()
        assertEquals("POST", pedido.method)
        assertEquals("/api/v1/viajes/TR-1042/cancelar", pedido.path)
        assertEquals(0L, pedido.bodySize)
    }

    @Test
    fun elAdministradorCancelaConMotivoYMonto() = runBlocking {
        servidor.enqueue(MockResponse().setBody(viaje))

        api().cancelarComoAdministrador("TR-1042", CancelarRequest("Cambio de planes", 25000))

        assertEquals("""{"motivo":"Cambio de planes","montoReembolso":25000}""", servidor.takeRequest().body.readUtf8())
    }

    @Test
    fun elPerfilSinTelefonoNoEnviaLaClave() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"nombre":"Ana","correo":"ana@x.cl","telefono":null,"fechaRegistro":"2026-11-01T10:00:00"}"""))

        api().actualizarUsuario("ana@x.cl", UsuarioRequest("Ana", "ana@x.cl", null))

        val pedido = servidor.takeRequest()
        assertEquals("PUT", pedido.method)
        assertFalse(pedido.body.readUtf8().contains("telefono"))
    }

    @Test
    fun elCorreoViajaDecodificadoEnLaRuta() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"nombre":"Ana","correo":"ana@x.cl","telefono":null,"fechaRegistro":"2026-11-01T10:00:00"}"""))

        api().usuario("ana@x.cl")

        assertEquals("ana@x.cl", servidor.takeRequest().requestUrl!!.pathSegments.last())
    }

    @Test
    fun lasListasDeViajesSoloLlevanLosFiltrosPresentes() = runBlocking {
        servidor.enqueue(MockResponse().setBody("[]"))
        servidor.enqueue(MockResponse().setBody("[]"))

        api().viajes(null, null)
        api().viajes("PENDIENTE", "2026-11-14")

        assertEquals("/api/v1/viajes", servidor.takeRequest().path)
        assertEquals("/api/v1/viajes?estado=PENDIENTE&fecha=2026-11-14", servidor.takeRequest().path)
    }

    @Test
    fun lasHabilitacionesSeFiltranPorPatenteOCorreo() = runBlocking {
        servidor.enqueue(MockResponse().setBody("[]"))

        api().habilitaciones(null, "c@x.cl")

        assertEquals("/api/v1/habilitaciones-movil-conductor?correo=c%40x.cl", servidor.takeRequest().path)
    }

    @Test
    fun elTokenDeNotificacionesSeEnviaYElServidorResponde204() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(204))

        api().tokenFcm(TokenFcmRequest("abc"))

        val pedido = servidor.takeRequest()
        assertEquals("/api/v1/auth/token-fcm", pedido.path)
        assertEquals("""{"tokenFcm":"abc"}""", pedido.body.readUtf8())
    }

    @Test
    fun elCierreDeSesionAceptaUnaRespuestaVacia() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(204))

        api().logout()

        assertEquals("/api/v1/auth/logout", servidor.takeRequest().path)
    }

    @Test
    fun activarOCerrarUnConductorEnviaElIndicador() = runBlocking {
        servidor.enqueue(
            MockResponse().setBody(
                """{"nombre":"Carlos","correo":"c@x.cl","telefono":null,"fechaRegistro":"2026-11-01T10:00:00","activo":false,
                    "estadoConductor":"DISPONIBLE","viajesAsignados":0,"folioEnCurso":null}""",
            ),
        )

        val conductor = api().cambiarActivoConductor("c@x.cl", ActivoRequest(false))

        assertTrue(!conductor.activo)
        assertEquals("""{"activo":false}""", servidor.takeRequest().body.readUtf8())
    }
}
