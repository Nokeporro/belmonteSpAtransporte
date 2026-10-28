package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.dao.UsuarioDao
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.MotivoSinPermiso
import cl.belmontespa.myapplication.model.PasswordTemporal
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.model.UsuarioEntity
import cl.belmontespa.myapplication.session.SesionStore
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Hashes
import cl.belmontespa.myapplication.util.Validaciones
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

open class AuthRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    private val usuarioDao: UsuarioDao get() = local.db.usuarioDao()
    private val sesionStore: SesionStore get() = local.sesiones
    private val clock: Clock get() = local.clock

    open fun observarSesion(): Flow<EstadoSesion> = sesionStore.estado

    open suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion> {
        val resultado = local.ejecutar {
            val usuario = usuarioDao.obtener(Validaciones.correo(correo))
                ?: return@ejecutar Resultado.Fallo(ErrorApp.SesionExpirada)
            val normal = usuario.passwordSha256 == Hashes.password(password)
            val temporal = usuario.rol in listOf("SOLICITANTE", "ADMINISTRADOR") &&
                usuario.passwordTemporalSha256 == Hashes.password(password) &&
                (usuario.passwordTemporalExpira ?: Long.MIN_VALUE) > clock.millis()
            if ((!normal && !temporal) || !usuario.activo) return@ejecutar Resultado.Fallo(ErrorApp.SesionExpirada)
            when (usuario.estadoSolicitante) {
                EstadoSolicitante.PENDIENTE.name ->
                    return@ejecutar Resultado.Fallo(ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_PENDIENTE))
                EstadoSolicitante.RECHAZADO.name ->
                    return@ejecutar Resultado.Fallo(ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_RECHAZADA))
            }
            val sesion = Sesion("demo-${UUID.randomUUID()}", usuario.correo, Rol.valueOf(usuario.rol),
                clock.millis() + DURACION_SESION.toMillis())
            if (normal) {
                usuarioDao.guardar(usuario.copy(passwordTemporalSha256 = null, passwordTemporalExpira = null))
                Resultado.Exito(EstadoSesion.Autenticada(sesion))
            } else Resultado.Exito(EstadoSesion.Temporal(sesion))
        }
        if (resultado is Resultado.Exito) when (val estado = resultado.valor) {
            is EstadoSesion.Temporal -> sesionStore.guardarTemporal(estado.sesion)
            is EstadoSesion.Autenticada -> sesionStore.guardar(estado.sesion)
            EstadoSesion.SinSesion -> Unit
        }
        return resultado
    }

    open suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> =
        local.ejecutar {
            val errores = Validaciones.registro(registro)
            if (errores.isNotEmpty()) return@ejecutar invalido(errores)
            if (usuarioDao.obtener(Validaciones.correo(registro.correo)) != null) {
                return@ejecutar conflicto("El correo ya está registrado.")
            }
            val rut = Hashes.normalizarDocumento(registro.rutEmpresa)
            val existentes = usuarioDao.observarPorRol("SOLICITANTE").first()
            val empresa = existentes.firstOrNull { Hashes.normalizarDocumento(it.rutEmpresa.orEmpty()) == rut }
            val u = UsuarioEntity(
                Validaciones.correo(registro.correo),
                registro.nombre.trim(),
                Validaciones.telefono(registro.telefono),
                "SOLICITANTE",
                true,
                null,
                if (empresa == null) "APROBADO" else "PENDIENTE",
                empresa?.razonSocial ?: registro.razonSocial.trim(),
                Validaciones.rut(rut),
                FechaIso.texto(local.ahora()),
                Hashes.password(registro.password),
            )
            usuarioDao.insertar(u)
            Resultado.Exito(aSolicitante(u))
        }

    open suspend fun recuperarPassword(correo: String): Resultado<PasswordTemporal> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val errores = Validaciones.recuperarPassword(correo)
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val usuario = local.buscarUsuario(Validaciones.correo(correo)) ?: return@ejecutar noEncontrado
        if (usuario.rol == Rol.CONDUCTOR.name) return@ejecutar noEncontrado
        val password = passwordTemporal()
        val expiraMillis = clock.millis() + VIGENCIA_TEMPORAL.toMillis()
        usuarioDao.guardar(usuario.copy(
            passwordTemporalSha256 = Hashes.password(password),
            passwordTemporalExpira = expiraMillis,
        ))
        Resultado.Exito(
            PasswordTemporal(usuario.correo, password, LocalDateTime.ofInstant(Instant.ofEpochMilli(expiraMillis), clock.zone)),
        )
    }

    open suspend fun actualizarTokenFcm(token: String): Resultado<Unit> = Resultado.Exito(Unit)

    open suspend fun cerrarSesion(): Resultado<Unit> {
        sesionStore.borrar()
        return Resultado.Exito(Unit)
    }

    private fun passwordTemporal(): String =
        String(CharArray(LARGO_PASSWORD_TEMPORAL) { ALFABETO[random.nextInt(ALFABETO.length)] })

    private companion object {
        val DURACION_SESION: Duration = Duration.ofHours(8)
        const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        const val LARGO_PASSWORD_TEMPORAL = 12
        val random = SecureRandom()
    }
}
