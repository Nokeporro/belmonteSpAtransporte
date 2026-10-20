package cl.belmontespa.myapplication.data.remote.repository

import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Solicitante
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Implementación remota (debug y release). Hasta T14 (Retrofit) toda llamada al backend responde
 * Sin conexión; la sesión guardada sí funciona.
 */
class AuthRemoteRepository @Inject constructor(
    private val sesionStore: SesionStore,
) : AuthRepository {

    override fun observarSesion(): Flow<EstadoSesion> = sesionStore.estado

    override suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun recuperarPassword(correo: String): Resultado<Unit> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun actualizarTokenFcm(token: String): Resultado<Unit> = Resultado.Fallo(ErrorApp.SinConexion)

    /** T14 agrega `POST /auth/logout` y el borrado de los viajes y la cola de Room. */
    override suspend fun cerrarSesion(): Resultado<Unit> {
        sesionStore.borrar()
        return Resultado.Exito(Unit)
    }
}
