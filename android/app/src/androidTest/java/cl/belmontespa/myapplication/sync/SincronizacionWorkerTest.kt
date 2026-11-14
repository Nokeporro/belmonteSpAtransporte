package cl.belmontespa.myapplication.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.model.EstadoEnvio
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.repository.api.ApiClient
import cl.belmontespa.myapplication.repository.viaje
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SincronizacionWorkerTest {

    @get:Rule
    val entorno = EntornoPruebas()

    private val servidor = MockWebServer()
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val cola get() = entorno.db.operacionPendienteDao()

    @Before
    fun preparar() {
        servidor.start()
        runBlocking { entorno.sesiones.guardar(sesionDeConductor()) }
        apuntarA(entorno.sesiones, servidor.url("/").toString())
    }

    @After
    fun cerrar() {
        ApiClient.reemplazarParaPruebas(entorno.sesiones, null)
        servidor.shutdown()
    }

    private suspend fun ejecutar() = TestListenableWorkerBuilder<SincronizacionWorker>(contexto).build().doWork()

    private suspend fun operaciones() = cola.observarDeCuenta(CONDUCTOR).first()

    @Test
    fun unaRespuesta2xxGuardaElViajeYBorraLaOperacion() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(200, viajeJson("TR-1042", "EN_CURSO"))

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        assertTrue(operaciones().isEmpty())
        assertEquals("EN_CURSO", entorno.local.viaje("TR-1042").estado)
        val pedido = servidor.takeRequest()
        assertEquals("POST", pedido.method)
        assertEquals("/api/v1/viajes/TR-1042/estado", pedido.path)
        assertEquals("""{"estado":"EN_CURSO"}""", pedido.body.readUtf8())
        assertEquals("Bearer jwt", pedido.getHeader("Authorization"))
    }

    @Test
    fun unErrorDelServidorReintentaYDejaLaOperacionPendiente() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(503)

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.retry(), resultado)
        assertEquals(EstadoEnvio.PENDIENTE.name, operaciones().single().estadoEnvio)
    }

    @Test
    fun sinRedReintentaYDejaLaOperacionPendiente() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        apuntarA(entorno.sesiones, "http://127.0.0.1:1/")

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.retry(), resultado)
        assertEquals(1, operaciones().size)
    }

    @Test
    fun un401EsperaALaMismaCuentaSinMarcarConflicto() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(401, """{"message":"Token inválido"}""")

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        assertEquals(EstadoEnvio.PENDIENTE.name, operaciones().single().estadoEnvio)
        assertEquals(EstadoSesion.SinSesion, entorno.sesiones.estado.first())
    }

    @Test
    fun un409ConElViajeYaEnElDestinoBorraLaOperacion() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(409, """{"message":"El viaje ya está en curso"}""")
        servidor.responder(200, viajeJson("TR-1042", "EN_CURSO"))

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        assertTrue(operaciones().isEmpty())
        assertEquals("EN_CURSO", entorno.local.viaje("TR-1042").estado)
        servidor.takeRequest()
        assertEquals("/api/v1/viajes/TR-1042", servidor.takeRequest().path)
    }

    @Test
    fun un409ConElViajeEnOtroEstadoQuedaEnConflictoConElMensaje() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(409, """{"message":"El viaje fue cancelado"}""")
        servidor.responder(200, viajeJson("TR-1042", "CANCELADO"))

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        val operacion = operaciones().single()
        assertEquals(EstadoEnvio.CONFLICTO.name, operacion.estadoEnvio)
        assertEquals("El viaje fue cancelado", operacion.mensajeError)
        assertEquals("CANCELADO", entorno.local.viaje("TR-1042").estado)
    }

    @Test
    fun otro4xxQuedaEnConflictoConElMensajeDelServidor() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(400, """{"message":"estado: Es obligatorio."}""")

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        val operacion = operaciones().single()
        assertEquals(EstadoEnvio.CONFLICTO.name, operacion.estadoEnvio)
        assertEquals("estado: Es obligatorio.", operacion.mensajeError)
    }

    @Test
    fun unConflictoSinMensajeGuardaElErrorNulo() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(404)

        ejecutar()

        val operacion = operaciones().single()
        assertEquals(EstadoEnvio.CONFLICTO.name, operacion.estadoEnvio)
        assertNull(operacion.mensajeError)
    }

    @Test
    fun unConflictoDetieneLasOperacionesDelMismoViajePeroNoLasDeOtro() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        cola.insertar(operacion("op-2", "TR-1042", EstadoViaje.FINALIZADO, creadoEn = 2))
        cola.insertar(operacion("op-3", "TR-1049", EstadoViaje.EN_CURSO, creadoEn = 3))
        servidor.responder(409, """{"message":"El viaje fue cancelado"}""")
        servidor.responder(200, viajeJson("TR-1042", "CANCELADO"))
        servidor.responder(200, viajeJson("TR-1049", "EN_CURSO"))

        ejecutar()

        assertEquals(
            listOf("/api/v1/viajes/TR-1042/estado", "/api/v1/viajes/TR-1042", "/api/v1/viajes/TR-1049/estado"),
            List(3) { servidor.takeRequest().path },
        )
        assertEquals(
            mapOf("op-1" to EstadoEnvio.CONFLICTO.name, "op-2" to EstadoEnvio.PENDIENTE.name),
            operaciones().associate { it.id to it.estadoEnvio },
        )
    }

    @Test
    fun unViajeYaEnConflictoNoEnviaSusOperacionesSiguientes() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1, envio = EstadoEnvio.CONFLICTO))
        cola.insertar(operacion("op-2", "TR-1042", EstadoViaje.FINALIZADO, creadoEn = 2))

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        assertEquals(0, servidor.requestCount)
        assertEquals(2, operaciones().size)
    }

    @Test
    fun lasOperacionesSeEnvianEnElOrdenEnQueSeCrearon() = runBlocking {
        cola.insertar(operacion("op-b", "TR-1049", EstadoViaje.EN_CURSO, creadoEn = 1))
        cola.insertar(operacion("op-a", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 2))
        servidor.responder(200, viajeJson("TR-1049", "EN_CURSO"))
        servidor.responder(200, viajeJson("TR-1042", "EN_CURSO"))

        ejecutar()

        assertEquals("/api/v1/viajes/TR-1049/estado", servidor.takeRequest().path)
        assertEquals("/api/v1/viajes/TR-1042/estado", servidor.takeRequest().path)
        assertTrue(operaciones().isEmpty())
    }

    @Test
    fun noEnviaLasOperacionesDeOtraCuenta() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1, cuenta = "otro@belmontespa.cl"))

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        assertEquals(0, servidor.requestCount)
        assertEquals(1, cola.observarDeCuenta("otro@belmontespa.cl").first().size)
    }

    @Test
    fun sinSesionLaColaEsperaSinLlamarAlServidor() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        entorno.sesiones.borrar()

        val resultado = ejecutar()

        assertEquals(ListenableWorker.Result.success(), resultado)
        assertEquals(0, servidor.requestCount)
        assertEquals(1, operaciones().size)
    }
}
