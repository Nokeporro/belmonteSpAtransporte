package cl.belmontespa.myapplication.model

import java.time.LocalDateTime

/** Contraseña temporal que genera un Administrador (CU-13): se muestra una sola vez y vence a las 24 horas. */
data class PasswordTemporal(val correo: String, val password: String, val expira: LocalDateTime)

/** Perfil de la cuenta propia; los campos de empresa solo los tiene el Solicitante. */
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

/** Autorregistro del Solicitante (RF01). */
data class RegistroSolicitante(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val password: String,
    val razonSocial: String,
    val rutEmpresa: String,
)

/** Alta de un Conductor o de un Administrador por el Administrador. */
data class NuevaCuenta(
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val password: String,
)
