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
        val usuario = usuarioDao.obtener(correo.trim().lowercase())
        // Mismo 401 genérico que el backend: correo desconocido, contraseña incorrecta o Conductor inactivo.
        if (usuario == null || usuario.passwordSha256 != Hashes.password(password) || !usuario.activo) {
            return Resultado.Fallo(ErrorApp.SesionExpirada)
        }
        when (usuario.estadoSolicitante) {
            EstadoSolicitante.PENDIENTE.name ->
                return Resultado.Fallo(ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_PENDIENTE))
            EstadoSolicitante.RECHAZADO.name ->
                return Resultado.Fallo(ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_RECHAZADA))
        }
        val sesion = Sesion(
            token = "demo-${UUID.randomUUID()}",
            correo = usuario.correo,
            rol = Rol.valueOf(usuario.rol),
            expiraEn = clock.millis() + DURACION_SESION.toMillis(),
        )
        sesionStore.guardar(sesion)
        return Resultado.Exito(EstadoSesion.Autenticada(sesion))
    }

    override suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> =
        local.ejecutar {
            exigir(registro.nombre.isNotBlank() && registro.correo.contains('@') && registro.password.length in 8..72 && registro.razonSocial.isNotBlank() && registro.rutEmpresa.isNotBlank(), "Revisa los datos ingresados.")
            val rut = Hashes.normalizarDocumento(registro.rutEmpresa)
            val existentes = usuarioDao.observarPorRol("SOLICITANTE").first()
            val empresa = existentes.firstOrNull { Hashes.normalizarDocumento(it.rutEmpresa.orEmpty()) == rut }
            val u = cl.belmontespa.myapplication.data.local.entity.UsuarioEntity(
                registro.correo.trim().lowercase(), registro.nombre.trim(), registro.telefono,
                "SOLICITANTE", true, null, if (empresa == null) "APROBADO" else "PENDIENTE",
                empresa?.razonSocial ?: registro.razonSocial.trim(), rut,
                cl.belmontespa.myapplication.util.FechaIso.texto(local.ahora()), Hashes.password(registro.password),
            )
            usuarioDao.insertar(u)
            u.solicitante()
        }

    /** La build demo no envía correos: "Recuperar contraseña" solo muestra su confirmación. */
    override suspend fun recuperarPassword(correo: String): Resultado<Unit> = Resultado.Exito(Unit)

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
