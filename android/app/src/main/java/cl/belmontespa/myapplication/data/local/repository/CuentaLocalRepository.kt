package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.local.entity.UsuarioEntity
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// Los perfiles y formularios de T06 leen la cuenta real de la sesión demo.
class CuentaLocalRepository @Inject constructor(private val local: SoporteLocal) : CuentaRepository {
    override suspend fun obtenerPerfil(): Resultado<Cuenta> = local.ejecutar {
        local.usuario(local.sesion(permitirTemporal = true).correo).cuenta()
    }
    override suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = local.ejecutar {
        exigir(nombre.isNotBlank(), "Ingresa el nombre.")
        val u = local.usuario(local.sesion().correo).copy(nombre = nombre.trim(), telefono = telefono)
        local.db.usuarioDao().guardar(u)
        u.cuenta()
    }
    override suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> {
        val s = try { local.sesion(permitirTemporal = true) } catch (e: ReglaLocal) { return Resultado.Fallo(e.error) }
        val resultado = local.ejecutar {
            exigir(s.rol != Rol.CONDUCTOR, "La contraseña del conductor la administra un Administrador.")
            val u = local.usuario(s.correo)
            exigir(u.passwordSha256 == Hashes.password(passwordActual), "La contraseña actual no es correcta.")
            exigir(passwordNueva.length in 8..72, "La contraseña debe tener entre 8 y 72 caracteres.")
            local.db.usuarioDao().guardar(u.copy(passwordSha256 = Hashes.password(passwordNueva)))
        }
        // Solo sale del flujo obligatorio después de persistir el cambio.
        if (resultado is Resultado.Exito) local.sesiones.guardar(s)
        return resultado
    }
    override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        local.db.usuarioDao().observarPorRol("SOLICITANTE").first()
            .filter { it.estadoSolicitante == "PENDIENTE" }.map { it.solicitante() }
    }
    private suspend fun aprobar(correo: String, estado: EstadoSolicitante): Resultado<Solicitante> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val u = local.usuario(correo)
        exigir(u.rol == "SOLICITANTE" && u.estadoSolicitante == "PENDIENTE", "La cuenta ya fue revisada.")
        val nueva = u.copy(estadoSolicitante = estado.name)
        local.db.usuarioDao().guardar(nueva)
        nueva.solicitante()
    }
    override suspend fun aprobarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.APROBADO)
    override suspend fun rechazarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.RECHAZADO)
    override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        exigir(cuenta.nombre.isNotBlank() && cuenta.correo.contains('@') && cuenta.password.length in 8..72, "Revisa los datos ingresados.")
        val u = UsuarioEntity(cuenta.correo.trim().lowercase(), cuenta.nombre.trim(), cuenta.telefono,
            "ADMINISTRADOR", true, null, null, null, null, FechaIso.texto(local.ahora()), Hashes.password(cuenta.password))
        local.db.usuarioDao().insertar(u)
        u.cuenta()
    }
}
