package cl.belmontespa.myapplication.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.model.CambioPendiente
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoEnvio
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.repository.AuthRepository
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.repository.api.ApiClient
import cl.belmontespa.myapplication.repository.viaje
import cl.belmontespa.myapplication.util.Reloj
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
class ColaDeCambiosTest {

    @get:Rule
    val entorno = EntornoPruebas()

    private val servidor = MockWebServer()
    private val programador = ProgramadorFalso()
    private lateinit var viajes: ViajeRepository

    private val cola get() = entorno.db.operacionPendienteDao()

    @Before
    fun preparar() {
        servidor.start()
        val local = SoporteLocal(entorno.db, entorno.sesiones, Reloj.santiago, modoLocal = false, programador = programador)
        viajes = ViajeRepository(local)
        runBlocking { entorno.sesiones.guardar(sesionDeConductor()) }
        apuntarA(entorno.sesiones, servidor.url("/").toString())
    }

    @After
    fun cerrar() {
        ApiClient.reemplazarParaPruebas(entorno.sesiones, null)
        servidor.shutdown()
    }

    private fun sinRed() = apuntarA(entorno.sesiones, "http://127.0.0.1:1/")

    private suspend fun operaciones() = cola.observarDeCuenta(CONDUCTOR).first()

    @Test
    fun sinRedElInicioDelConductorSeEncolaYSeProgramaElEnvio() = runBlocking {
        sinRed()

        val resultado = viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO)

        assertEquals(Resultado.Encolado, resultado)
        val operacion = operaciones().single()
        assertEquals("TR-1042", operacion.folio)
        assertEquals(EstadoViaje.EN_CURSO.name, operacion.estadoDestino)
        assertEquals(EstadoEnvio.PENDIENTE.name, operacion.estadoEnvio)
        assertEquals(1, programador.llamadas)
        assertEquals("ASIGNADO", entorno.local.viaje("TR-1042").estado)
    }

    @Test
    fun elViajeMuestraElCambioPendienteSinCambiarSuEstado() = runBlocking {
        sinRed()

        viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO)

        val viaje = checkNotNull(viajes.observarViaje("TR-1042").first())
        assertEquals(EstadoViaje.ASIGNADO, viaje.estado)
        assertEquals(CambioPendiente(EstadoViaje.EN_CURSO), viaje.cambioPendiente)
    }

    @Test
    fun conOperacionesPendientesLaSiguienteSeEncolaSinLlamarALaApi() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))

        val resultado = viajes.cambiarEstado("TR-1042", EstadoViaje.FINALIZADO)

        assertEquals(Resultado.Encolado, resultado)
        assertEquals(0, servidor.requestCount)
        assertEquals(listOf("EN_CURSO", "FINALIZADO"), operaciones().map { it.estadoDestino })
    }

    @Test
    fun conRedElCambioSeEnviaDeInmediatoYNoSeEncola() = runBlocking {
        servidor.responder(200, viajeJson("TR-1042", "EN_CURSO"))

        val resultado = viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO)

        assertEquals(Resultado.Exito(Unit), resultado)
        assertTrue(operaciones().isEmpty())
        assertEquals(0, programador.llamadas)
        assertEquals("EN_CURSO", entorno.local.viaje("TR-1042").estado)
    }

    @Test
    fun unErrorDelServidorEnElEnvioDirectoSeMuestraYNoSeEncola() = runBlocking {
        servidor.responder(500)

        val resultado = viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO)

        assertEquals(Resultado.Fallo(ErrorApp.Inesperado), resultado)
        assertTrue(operaciones().isEmpty())
    }

    @Test
    fun soloSeEncolanIniciarYFinalizar() = runBlocking {
        sinRed()

        val resultado = viajes.cambiarEstado("TR-1042", EstadoViaje.CANCELADO)

        assertEquals(Resultado.Fallo(ErrorApp.SinConexion), resultado)
        assertTrue(operaciones().isEmpty())
    }

    @Test
    fun unSolicitanteNoEncolaNada() = runBlocking {
        entorno.sesiones.guardar(Sesion("jwt", "crios@andesconsult.cl", Rol.SOLICITANTE, System.currentTimeMillis() + 3_600_000))
        sinRed()

        val resultado = viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO)

        assertEquals(Resultado.Fallo(ErrorApp.SinConexion), resultado)
        assertEquals(0, programador.llamadas)
        assertTrue(cola.observarDelViaje("TR-1042").first().isEmpty())
    }

    @Test
    fun unViajeQueDejaDeExistirSeBorraAunqueTengaOperacionesPendientes() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(404, """{"message":"No encontramos este registro."}""")

        val resultado = viajes.refrescarViaje("TR-1042")

        assertEquals(Resultado.Fallo(ErrorApp.NoEncontrado), resultado)
        assertNull(entorno.local.buscarViaje("TR-1042"))
        assertTrue(cola.observarDelViaje("TR-1042").first().isEmpty())
    }

    @Test
    fun descartarElCambioBorraLasOperacionesYReconsultaElViaje() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1, envio = EstadoEnvio.CONFLICTO, mensaje = "El viaje fue cancelado"))
        servidor.responder(200, viajeJson("TR-1042", "CANCELADO"))

        val resultado = viajes.descartarCambio("TR-1042")

        assertEquals(Resultado.Exito(Unit), resultado)
        assertTrue(operaciones().isEmpty())
        assertEquals("/api/v1/viajes/TR-1042", servidor.takeRequest().path)
        assertEquals("CANCELADO", entorno.local.viaje("TR-1042").estado)
    }

    @Test
    fun cuentaLosCambiosSinEnviarDeLaSesion() = runBlocking {
        assertEquals(0, viajes.contarPendientes())

        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        cola.insertar(operacion("op-2", "TR-1049", EstadoViaje.EN_CURSO, creadoEn = 2, envio = EstadoEnvio.CONFLICTO))
        cola.insertar(operacion("op-3", "TR-1045", EstadoViaje.FINALIZADO, creadoEn = 3, cuenta = "otro@belmontespa.cl"))

        assertEquals(2, viajes.contarPendientes())
    }

    @Test
    fun sincronizarAhoraEnviaLaColaYAvisaSiNoPudo() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        servidor.responder(200, viajeJson("TR-1042", "EN_CURSO"))
        assertEquals(Resultado.Exito(Unit), viajes.sincronizarAhora())
        assertTrue(operaciones().isEmpty())

        cola.insertar(operacion("op-2", "TR-1042", EstadoViaje.FINALIZADO, creadoEn = 2))
        servidor.responder(503)
        assertEquals(Resultado.Fallo(ErrorApp.SinConexion), viajes.sincronizarAhora())

        servidor.responder(409, """{"message":"El viaje fue cancelado"}""")
        servidor.responder(200, viajeJson("TR-1042", "CANCELADO"))
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El viaje fue cancelado")), viajes.sincronizarAhora())
    }

    @Test
    fun alEntrarConLaMismaCuentaConPendientesSeProgramaElEnvio() = runBlocking {
        cola.insertar(operacion("op-1", "TR-1042", EstadoViaje.EN_CURSO, creadoEn = 1))
        val local = SoporteLocal(entorno.db, entorno.sesiones, Reloj.santiago, modoLocal = false, programador = programador)
        servidor.responder(
            200,
            """{"accessToken":"nuevo","tokenType":"Bearer","expiresIn":3600,"correo":"$CONDUCTOR","rol":"CONDUCTOR","cambioPasswordRequerido":false}""",
        )

        val resultado = AuthRepository(local).iniciarSesion(CONDUCTOR, "Clave12345")

        assertTrue(resultado is Resultado.Exito)
        assertEquals(1, programador.llamadas)
        assertEquals(1, operaciones().size)
    }
}

private class ProgramadorFalso : ProgramadorSincronizacion {
    var llamadas = 0

    override fun programar() {
        llamadas++
    }
}
