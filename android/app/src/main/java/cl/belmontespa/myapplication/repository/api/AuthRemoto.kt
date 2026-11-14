package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.PasswordTemporal
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.LoginRequest
import cl.belmontespa.myapplication.repository.dto.RecuperarPasswordRequest
import cl.belmontespa.myapplication.repository.dto.SolicitanteRequest
import cl.belmontespa.myapplication.repository.dto.TokenFcmRequest
import cl.belmontespa.myapplication.repository.dto.aModelo
import cl.belmontespa.myapplication.util.Validaciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal class AuthRemoto(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)

    suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion> = llamar {
        val respuesta = api.login(LoginRequest(Validaciones.correo(correo), password))
        val sesion = Sesion(
            respuesta.accessToken,
            respuesta.correo,
            Rol.valueOf(respuesta.rol),
            local.clock.millis() + respuesta.expiresIn * 1000,
        )
        if (local.sesiones.ultimaCuenta.first() != sesion.correo) local.db.viajeDao().borrarCache()
        if (respuesta.cambioPasswordRequerido) {
            local.sesiones.guardarTemporal(sesion)
            EstadoSesion.Temporal(sesion)
        } else {
            local.sesiones.guardar(sesion)
            if (local.db.operacionPendienteDao().contarDeLaCuenta(sesion.correo) > 0) local.programador.programar()
            EstadoSesion.Autenticada(sesion)
        }
    }

    suspend fun hayCambiosDeOtraCuenta(correo: String): Boolean =
        local.db.operacionPendienteDao().contarDeOtraCuenta(correo.trim()) > 0

    suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> = llamar {
        api.registrarSolicitante(
            SolicitanteRequest(
                nombre = registro.nombre.trim(),
                correo = Validaciones.correo(registro.correo),
                telefono = Validaciones.telefono(registro.telefono)?.takeIf { it.isNotBlank() },
                password = registro.password,
                razonSocial = registro.razonSocial.trim(),
                rutEmpresa = Validaciones.rut(registro.rutEmpresa),
            ),
        ).aModelo()
    }

    suspend fun recuperarPassword(correo: String): Resultado<PasswordTemporal> = llamar {
        api.recuperarPassword(RecuperarPasswordRequest(Validaciones.correo(correo))).aModelo()
    }

    suspend fun actualizarTokenFcm(token: String): Resultado<Unit> = llamar { api.tokenFcm(TokenFcmRequest(token)) }

    suspend fun cerrarSesion(): Resultado<Unit> {
        try {
            api.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        local.db.viajeDao().borrarCache()
        local.sesiones.borrar()
        return Resultado.Exito(Unit)
    }
}
