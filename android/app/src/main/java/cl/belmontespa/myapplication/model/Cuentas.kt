package cl.belmontespa.myapplication.model

import java.time.LocalDateTime

data class PasswordTemporal(val correo: String, val password: String, val expira: LocalDateTime)

data class Cuenta(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val rol: Rol,
    val fechaRegistro: LocalDateTime,
    val razonSocial: String? = null,
    val rutEmpresa: String? = null,
    val estadoSolicitante: EstadoSolicitante? = null,
)

data class Conductor(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val fechaRegistro: LocalDateTime,
    val activo: Boolean,
    val estado: EstadoConductor,
    val viajesAsignados: Int,
    val folioEnCurso: String?,
)

data class Solicitante(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val fechaRegistro: LocalDateTime,
    val razonSocial: String,
    val rutEmpresa: String,
    val estado: EstadoSolicitante,
)

data class RegistroSolicitante(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val password: String,
    val razonSocial: String,
    val rutEmpresa: String,
)

data class NuevaCuenta(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val password: String,
)
