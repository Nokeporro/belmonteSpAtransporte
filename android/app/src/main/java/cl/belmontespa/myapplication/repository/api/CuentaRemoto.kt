package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.AdministradorRequest
import cl.belmontespa.myapplication.repository.dto.LoginRequest
import cl.belmontespa.myapplication.repository.dto.UsuarioRequest
import cl.belmontespa.myapplication.repository.dto.aCuenta
import cl.belmontespa.myapplication.repository.dto.aModelo
import cl.belmontespa.myapplication.repository.obtener
import cl.belmontespa.myapplication.repository.sinPermiso
import cl.belmontespa.myapplication.util.Validaciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal class CuentaRemoto(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)

    suspend fun obtenerPerfil(): Resultado<Cuenta> = intentar {
        val sesion = local.sesion(permitirTemporal = true).obtener { return@intentar it }
        val temporal = local.sesiones.estado.first() is EstadoSesion.Temporal
        val usuario = api.usuario(sesion.correo)
        val solicitante = if (sesion.rol == Rol.SOLICITANTE && !temporal) api.solicitante(sesion.correo) else null
        Resultado.Exito(usuario.aCuenta(sesion.rol, solicitante))
    }

    suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = intentar {
        val sesion = local.sesion().obtener { return@intentar it }
        val usuario = api.actualizarUsuario(
            sesion.correo,
            UsuarioRequest(nombre.trim(), sesion.correo, Validaciones.telefono(telefono)?.takeIf { it.isNotBlank() }),
        )
        val solicitante = if (sesion.rol == Rol.SOLICITANTE) api.solicitante(sesion.correo) else null
        Resultado.Exito(usuario.aCuenta(sesion.rol, solicitante))
    }

    suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> = intentar {
        val sesion = local.sesion(permitirTemporal = true).obtener { return@intentar it }
        if (sesion.rol == Rol.CONDUCTOR) return@intentar sinPermiso()
        val perfil = api.usuario(sesion.correo)
        api.actualizarUsuario(
            sesion.correo,
            UsuarioRequest(perfil.nombre, sesion.correo, perfil.telefono, password = passwordNueva, passwordActual = passwordActual),
        )
        try {
            val respuesta = api.login(LoginRequest(sesion.correo, passwordNueva))
            local.sesiones.guardar(
                Sesion(respuesta.accessToken, respuesta.correo, Rol.valueOf(respuesta.rol), local.clock.millis() + respuesta.expiresIn * 1000),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            local.sesiones.borrar()
        }
        Resultado.Exito(Unit)
    }

    suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = llamar {
        api.solicitantes("PENDIENTE").map { it.aModelo() }
    }

    suspend fun aprobarSolicitante(correo: String): Resultado<Solicitante> = llamar {
        api.aprobarSolicitante(Validaciones.correo(correo)).aModelo()
    }

    suspend fun rechazarSolicitante(correo: String): Resultado<Solicitante> = llamar {
        api.rechazarSolicitante(Validaciones.correo(correo)).aModelo()
    }

    suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = llamar {
        api.registrarAdministrador(
            AdministradorRequest(
                cuenta.nombre.trim(),
                Validaciones.correo(cuenta.correo),
                Validaciones.telefono(cuenta.telefono)?.takeIf { it.isNotBlank() },
                cuenta.password,
            ),
        ).aCuenta(Rol.ADMINISTRADOR)
    }
}
