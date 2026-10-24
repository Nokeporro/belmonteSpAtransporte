package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.local.entity.UsuarioEntity
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Validaciones
import java.time.Duration
import javax.inject.Inject

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
            if (s.rol == Rol.CONDUCTOR) falla(ErrorApp.SinPermiso())
            validar(cl.belmontespa.myapplication.util.Validaciones.cambioPassword(passwordActual, passwordNueva, passwordNueva))
            val u = local.usuario(s.correo)
            val normal = u.passwordSha256 == Hashes.password(passwordActual)
            val temporal = u.passwordTemporalSha256 == Hashes.password(passwordActual) &&
                (u.passwordTemporalExpira ?: Long.MIN_VALUE) > local.clock.millis()
            if (!normal && !temporal) falla(ErrorApp.Validacion(mapOf("passwordActual" to "No coincide.")))
            local.db.usuarioDao().guardar(u.copy(passwordSha256 = Hashes.password(passwordNueva),
                passwordTemporalSha256 = null, passwordTemporalExpira = null))
        }
        // Solo sale del flujo obligatorio después de persistir el cambio.
        if (resultado is Resultado.Exito) local.sesiones.guardar(s)
        return resultado
    }
    override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val orden = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("es-CL"))
            .apply { strength = java.text.Collator.PRIMARY }
        local.db.usuarioDao().listarSolicitantesPendientes().sortedWith { a, b ->
            orden.compare(a.nombre, b.nombre).takeIf { it != 0 } ?: a.correo.compareTo(b.correo)
        }.map { it.solicitante() }
    }
    private suspend fun aprobar(correo: String, estado: EstadoSolicitante): Resultado<Solicitante> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val u = local.usuario(cl.belmontespa.myapplication.util.Validaciones.correo(correo))
        if (u.rol != "SOLICITANTE") falla(ErrorApp.NoEncontrado)
        exigir(u.estadoSolicitante == "PENDIENTE", "La cuenta ya fue revisada.")
        val nueva = u.copy(estadoSolicitante = estado.name)
        local.db.usuarioDao().guardar(nueva)
        nueva.solicitante()
    }
    override suspend fun aprobarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.APROBADO)
    override suspend fun rechazarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.RECHAZADO)
    override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        validar(Validaciones.administrador(cuenta))
        val correo = Validaciones.correo(cuenta.correo)
        if (local.db.usuarioDao().obtener(correo) != null) falla(ErrorApp.Conflicto("El correo ya está registrado."))
        val u = UsuarioEntity(correo, cuenta.nombre.trim(), Validaciones.telefono(cuenta.telefono),
            "ADMINISTRADOR", true, null, null, null, null, FechaIso.texto(local.ahora()), null,
            Hashes.password(cuenta.password), local.clock.millis() + Duration.ofHours(72).toMillis())
        local.db.usuarioDao().insertar(u)
        u.cuenta()
    }
}
