package cl.belmontespa.myapplication.sync

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ClasificacionEnvioTest {

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "".toResponseBody()))

    @Test
    fun sinRedOTiempoAgotadoSeReintenta() {
        assertEquals(SalidaDeEnvio.REINTENTAR, clasificar(IOException("sin red")))
        assertEquals(SalidaDeEnvio.REINTENTAR, clasificar(SocketTimeoutException("tiempo agotado")))
    }

    @Test
    fun unErrorDelServidorSeReintenta() {
        listOf(500, 502, 503, 599).forEach { assertEquals("HTTP $it", SalidaDeEnvio.REINTENTAR, clasificar(http(it))) }
    }

    @Test
    fun un401EsperaALaMismaCuenta() {
        assertEquals(SalidaDeEnvio.ESPERAR_SESION, clasificar(http(401)))
    }

    @Test
    fun un409SeVerificaConsultandoElViaje() {
        assertEquals(SalidaDeEnvio.VERIFICAR, clasificar(http(409)))
    }

    @Test
    fun cualquierOtro4xxEsConflicto() {
        listOf(400, 403, 404, 422).forEach { assertEquals("HTTP $it", SalidaDeEnvio.CONFLICTO, clasificar(http(it))) }
    }

    @Test
    fun unaRespuestaIlegibleOUnFalloInesperadoEsConflicto() {
        assertEquals(SalidaDeEnvio.CONFLICTO, clasificar(SerializationException("json roto")))
        assertEquals(SalidaDeEnvio.CONFLICTO, clasificar(IllegalStateException("estado imposible")))
    }
}
