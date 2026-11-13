package cl.belmontespa.myapplication.repository.dto

import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.PasswordTemporal
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Solicitante
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val correo: String, val password: String, val tokenFcm: String? = null)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val correo: String,
    val rol: String,
    val cambioPasswordRequerido: Boolean,
)

@Serializable
data class TokenFcmRequest(val tokenFcm: String)

@Serializable
data class RecuperarPasswordRequest(val correo: String)

@Serializable
data class PasswordTemporalResponse(
    val correo: String,
    val passwordTemporal: String,
    @Serializable(with = SerializadorFecha::class) val expira: LocalDateTime,
)

@Serializable
data class SolicitanteRequest(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val password: String,
    val razonSocial: String,
    val rutEmpresa: String,
)

@Serializable
data class SolicitanteResponse(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    @Serializable(with = SerializadorFecha::class) val fechaRegistro: LocalDateTime,
    val razonSocial: String,
    val rutEmpresa: String,
    val estadoSolicitante: String,
)

@Serializable
data class UsuarioRequest(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val password: String? = null,
    val passwordActual: String? = null,
)

@Serializable
data class UsuarioResponse(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    @Serializable(with = SerializadorFecha::class) val fechaRegistro: LocalDateTime,
)

@Serializable
data class AdministradorRequest(val nombre: String, val correo: String, val telefono: String?, val password: String)

fun PasswordTemporalResponse.aModelo() = PasswordTemporal(correo, passwordTemporal, expira)

fun SolicitanteResponse.aModelo() = Solicitante(
    nombre, correo, telefono, fechaRegistro, razonSocial, rutEmpresa, EstadoSolicitante.valueOf(estadoSolicitante),
)

fun UsuarioResponse.aCuenta(rol: Rol, solicitante: SolicitanteResponse? = null) = Cuenta(
    nombre = nombre,
    correo = correo,
    telefono = telefono,
    rol = rol,
    fechaRegistro = fechaRegistro,
    razonSocial = solicitante?.razonSocial,
    rutEmpresa = solicitante?.rutEmpresa,
    estadoSolicitante = solicitante?.estadoSolicitante?.let(EstadoSolicitante::valueOf),
)
