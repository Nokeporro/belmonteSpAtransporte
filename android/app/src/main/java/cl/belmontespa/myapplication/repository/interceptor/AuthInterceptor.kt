package cl.belmontespa.myapplication.repository.interceptor

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.session.SesionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AuthInterceptor(private val sesiones: SesionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val pedido = chain.request()
        if (esPublica(pedido)) return chain.proceed(pedido)
        val sesion = when (val estado = runBlocking { sesiones.estado.first() }) {
            is EstadoSesion.Autenticada -> estado.sesion
            is EstadoSesion.Temporal -> estado.sesion
            EstadoSesion.SinSesion -> null
        }
        val conToken = sesion?.let { pedido.newBuilder().header("Authorization", "Bearer ${it.token}").build() } ?: pedido
        val respuesta = chain.proceed(conToken)
        if (respuesta.code == 401 && sesion != null) runBlocking { sesiones.borrar() }
        return respuesta
    }

    private fun esPublica(pedido: Request): Boolean =
        pedido.method == "POST" && RUTAS_PUBLICAS.any { pedido.url.encodedPath.endsWith(it) }

    private companion object {
        val RUTAS_PUBLICAS = listOf("/api/v1/auth/login", "/api/v1/solicitantes", "/api/v1/abordajes")
    }
}
