package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.local.dao.UsuarioDao
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.MotivoSinPermiso
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Solicitante
import java.time.Clock
import java.time.Duration
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Login de la build demo contra las cuentas ficticias de Room; la sesión se guarda igual que con el backend. */
class AuthLocalRepository @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val sesionStore: SesionStore,
    private val clock: Clock,
    private val local: SoporteLocal,
) : AuthRepository {

    override fun observarSesion(): Flow<EstadoSesion> = sesionStore.estado

    override suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion> {
        val resultado = local.ejecutar {
            val usuario = usuarioDao.obtener(cl.belmontespa.myapplication.util.Validaciones.correo(correo))
                ?: falla(ErrorApp.SesionExpirada)
            val normal = usuario.passwordSha256 == Hashes.password(password)
            val temporal = usuario.rol in listOf("SOLICITANTE", "ADMINISTRADOR") &&
                usuario.passwordTemporalSha256 == Hashes.password(password) &&
                (usuario.passwordTemporalExpira ?: Long.MIN_VALUE) > clock.millis()
            if ((!normal && !temporal) || !usuario.activo) falla(ErrorApp.SesionExpirada)
            when (usuario.estadoSolicitante) {
                EstadoSolicitante.PENDIENTE.name -> falla(ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_PENDIENTE))
                EstadoSolicitante.RECHAZADO.name -> falla(ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_RECHAZADA))
            }
            val sesion = Sesion("demo-${UUID.randomUUID()}", usuario.correo, Rol.valueOf(usuario.rol),
                clock.millis() + DURACION_SESION.toMillis())
            if (normal) {
                usuarioDao.guardar(usuario.copy(passwordTemporalSha256 = null, passwordTemporalExpira = null))
                EstadoSesion.Autenticada(sesion)
            } else EstadoSesion.Temporal(sesion)
        }
        if (resultado is Resultado.Exito) when (val estado = resultado.valor) {
            is EstadoSesion.Temporal -> sesionStore.guardarTemporal(estado.sesion)
            is EstadoSesion.Autenticada -> sesionStore.guardar(estado.sesion)
            EstadoSesion.SinSesion -> Unit
        }
        return resultado
    }

    override suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> =
        local.ejecutar {
            validar(cl.belmontespa.myapplication.util.Validaciones.registro(registro))
            if (usuarioDao.obtener(cl.belmontespa.myapplication.util.Validaciones.correo(registro.correo)) != null) falla(ErrorApp.Conflicto("El correo ya está registrado."))
            val rut = Hashes.normalizarDocumento(registro.rutEmpresa)
            val existentes = usuarioDao.observarPorRol("SOLICITANTE").first()
            val empresa = existentes.firstOrNull { Hashes.normalizarDocumento(it.rutEmpresa.orEmpty()) == rut }
            val u = cl.belmontespa.myapplication.data.local.entity.UsuarioEntity(
                cl.belmontespa.myapplication.util.Validaciones.correo(registro.correo), registro.nombre.trim(), cl.belmontespa.myapplication.util.Validaciones.telefono(registro.telefono),
                "SOLICITANTE", true, null, if (empresa == null) "APROBADO" else "PENDIENTE",
                empresa?.razonSocial ?: registro.razonSocial.trim(), cl.belmontespa.myapplication.util.Validaciones.rut(rut),
                cl.belmontespa.myapplication.util.FechaIso.texto(local.ahora()), Hashes.password(registro.password),
            )
            usuarioDao.insertar(u)
            u.solicitante()
        }

    /** La build demo no envía correos: "Recuperar contraseña" solo muestra su confirmación. */
    override suspend fun recuperarPassword(correo: String): Resultado<Unit> = local.ejecutar {
        validar(cl.belmontespa.myapplication.util.Validaciones.recuperarPassword(correo))
    }

    /** La build demo no tiene push. */
    override suspend fun actualizarTokenFcm(token: String): Resultado<Unit> = Resultado.Exito(Unit)

    /** Cierra la sesión local; en la build demo Room se conserva, porque es su base de datos. */
    override suspend fun cerrarSesion(): Resultado<Unit> {
        sesionStore.borrar()
        return Resultado.Exito(Unit)
    }

    private companion object {
        /** Igual que el JWT del backend: 8 horas. */
        val DURACION_SESION: Duration = Duration.ofHours(8)
    }
}
