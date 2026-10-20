package cl.belmontespa.myapplication.ui.navigation

import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Solicitante
import kotlinx.coroutines.flow.MutableStateFlow

/** Doble de prueba escrito a mano: la prueba fija la sesión y observa la navegación. */
class AuthFalso : AuthRepository {
    val sesion = MutableStateFlow<EstadoSesion>(EstadoSesion.SinSesion)

    override fun observarSesion() = sesion

    override suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion> =
        Resultado.Fallo(ErrorApp.SesionExpirada)

    override suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> =
        Resultado.Fallo(ErrorApp.Inesperado)

    override suspend fun recuperarPassword(correo: String): Resultado<Unit> = Resultado.Exito(Unit)

    override suspend fun actualizarTokenFcm(token: String): Resultado<Unit> = Resultado.Exito(Unit)

    override suspend fun cerrarSesion(): Resultado<Unit> {
        sesion.value = EstadoSesion.SinSesion
        return Resultado.Exito(Unit)
    }
}
