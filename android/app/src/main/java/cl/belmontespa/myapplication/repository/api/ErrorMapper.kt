package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.MotivoSinPermiso
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class ErrorApi(val message: String? = null)

object ErrorMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val LINEA_DE_CAMPO = Regex("""^([\w.]+): (.+)$""")

    fun aErrorApp(error: Throwable): ErrorApp = when (error) {
        is HttpException -> desdeHttp(error.code(), error.response()?.errorBody()?.string())
        is IOException -> ErrorApp.SinConexion
        else -> ErrorApp.Inesperado
    }

    fun desdeHttp(codigo: Int, cuerpo: String?): ErrorApp {
        val mensaje = cuerpo?.let { runCatching { json.decodeFromString<ErrorApi>(it).message }.getOrNull() }
        return when (codigo) {
            400 -> ErrorApp.Validacion(campos(mensaje))
            401 -> ErrorApp.SesionExpirada
            403 -> ErrorApp.SinPermiso(motivo(mensaje))
            404 -> ErrorApp.NoEncontrado
            409 -> ErrorApp.Conflicto(mensaje ?: "La operación no se pudo completar.")
            else -> ErrorApp.Inesperado
        }
    }

    private fun campos(mensaje: String?): Map<String, String> {
        val lineas = mensaje?.lines()?.filter { it.isNotBlank() }.orEmpty()
        val encontradas = lineas.mapNotNull { LINEA_DE_CAMPO.matchEntire(it)?.destructured }
        if (lineas.isEmpty() || encontradas.size != lineas.size) {
            return mapOf("datos" to "Revisa los datos ingresados.")
        }
        return encontradas.associate { (campo, motivo) -> campo to motivo }
    }

    private fun motivo(mensaje: String?): MotivoSinPermiso = when {
        mensaje == null -> MotivoSinPermiso.GENERAL
        "espera la aprobación" in mensaje -> MotivoSinPermiso.CUENTA_PENDIENTE
        "no fue aprobada" in mensaje -> MotivoSinPermiso.CUENTA_RECHAZADA
        "cambiar la contraseña" in mensaje -> MotivoSinPermiso.CAMBIAR_PASSWORD
        else -> MotivoSinPermiso.GENERAL
    }
}
