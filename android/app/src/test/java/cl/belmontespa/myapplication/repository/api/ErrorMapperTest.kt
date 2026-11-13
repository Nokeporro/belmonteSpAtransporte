package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.MotivoSinPermiso
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErrorMapperTest {

    private fun http(codigo: Int, cuerpo: String = ""): HttpException =
        HttpException(Response.error<Unit>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    private fun mensaje(texto: String) = """{"status":403,"error":"Forbidden","message":"$texto","path":"/api/v1/x"}"""

    @Test
    fun un400ConUnaLineaPorCampoSeSeparaEnErroresPorCampo() {
        val error = ErrorMapper.aErrorApp(http(400, mensaje("dni: Ingresa un RUT válido.")))

        assertEquals(ErrorApp.Validacion(mapOf("dni" to "Ingresa un RUT válido.")), error)
    }

    @Test
    fun un400DeVariasLineasConservaCadaCampo() {
        val error = ErrorMapper.aErrorApp(http(400, mensaje("fhAgenda: Debe ser una fecha futura.\\norigen.comuna: No existe en el catálogo.")))

        assertEquals(
            ErrorApp.Validacion(
                mapOf("fhAgenda" to "Debe ser una fecha futura.", "origen.comuna" to "No existe en el catálogo."),
            ),
            error,
        )
    }

    @Test
    fun un400DeTextoLibreOSinCuerpoVaAlCampoDatos() {
        val esperado = ErrorApp.Validacion(mapOf("datos" to "Revisa los datos ingresados."))

        assertEquals(esperado, ErrorMapper.aErrorApp(http(400, mensaje("La contraseña es obligatoria"))))
        assertEquals(esperado, ErrorMapper.aErrorApp(http(400)))
        assertEquals(esperado, ErrorMapper.aErrorApp(http(400, "no es json")))
    }

    @Test
    fun un401EsSesionExpirada() {
        assertEquals(ErrorApp.SesionExpirada, ErrorMapper.aErrorApp(http(401)))
    }

    @Test
    fun un403TraeElMotivoSegunElMensaje() {
        assertEquals(
            ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_PENDIENTE),
            ErrorMapper.aErrorApp(http(403, mensaje("Tu cuenta espera la aprobación de un Administrador."))),
        )
        assertEquals(
            ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_RECHAZADA),
            ErrorMapper.aErrorApp(http(403, mensaje("Tu cuenta no fue aprobada. Contacta a Belmonte SpA."))),
        )
        assertEquals(
            ErrorApp.SinPermiso(MotivoSinPermiso.CAMBIAR_PASSWORD),
            ErrorMapper.aErrorApp(http(403, mensaje("Debes cambiar la contraseña"))),
        )
        assertEquals(ErrorApp.SinPermiso(MotivoSinPermiso.GENERAL), ErrorMapper.aErrorApp(http(403)))
    }

    @Test
    fun un404EsNoEncontrado() {
        assertEquals(ErrorApp.NoEncontrado, ErrorMapper.aErrorApp(http(404)))
    }

    @Test
    fun un409LlevaElMensajeDelServidor() {
        assertEquals(
            ErrorApp.Conflicto("El viaje está vencido"),
            ErrorMapper.aErrorApp(http(409, mensaje("El viaje está vencido"))),
        )
        assertEquals(ErrorApp.Conflicto("La operación no se pudo completar."), ErrorMapper.aErrorApp(http(409)))
    }

    @Test
    fun el405ElCincuentaYCualquierOtroCodigoSonInesperados() {
        for (codigo in listOf(405, 418, 500, 503)) {
            assertEquals(codigo.toString(), ErrorApp.Inesperado, ErrorMapper.aErrorApp(http(codigo)))
        }
    }

    @Test
    fun laRedYElTiempoAgotadoSonSinConexion() {
        assertEquals(ErrorApp.SinConexion, ErrorMapper.aErrorApp(IOException("sin red")))
        assertEquals(ErrorApp.SinConexion, ErrorMapper.aErrorApp(SocketTimeoutException("timeout")))
    }

    @Test
    fun cualquierOtroFalloEsInesperado() {
        assertEquals(ErrorApp.Inesperado, ErrorMapper.aErrorApp(IllegalArgumentException("estado desconocido")))
    }
}
