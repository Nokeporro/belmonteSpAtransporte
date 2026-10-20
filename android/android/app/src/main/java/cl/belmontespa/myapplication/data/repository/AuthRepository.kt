package cl.belmontespa.myapplication.data.repository

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Solicitante
import kotlinx.coroutines.flow.Flow

/** Sesión, login, autorregistro, recuperación de contraseña y token de notificaciones. */
interface AuthRepository {
    fun observarSesion(): Flow<EstadoSesion>

    /** Devuelve `Autenticada` o, si entró con la contraseña temporal, `Temporal`. */
    suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion>

    suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante>

    suspend fun recuperarPassword(correo: String): Resultado<Unit>

    suspend fun actualizarTokenFcm(token: String): Resultado<Unit>

    /** Siempre termina la sesión local, aunque falle el aviso al servidor. */
    suspend fun cerrarSesion(): Resultado<Unit>
}
