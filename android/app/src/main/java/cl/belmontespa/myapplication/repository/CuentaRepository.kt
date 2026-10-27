package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.model.UsuarioEntity
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Hashes
import cl.belmontespa.myapplication.util.Validaciones
import java.text.Collator
import java.time.Duration
import java.util.Locale

/**
 * Cuenta propia (la de la sesión), aprobación de Solicitantes y alta de Administradores.
 *
 * Los perfiles y formularios de T06 leen la cuenta real de la sesión demo.
 */
open class CuentaRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    open suspend fun obtenerPerfil(): Resultado<Cuenta> = local.ejecutar {
        local.usuario(local.sesion(permitirTemporal = true).correo).cuenta()
    }
    open suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = local.ejecutar {
        exigir(nombre.isNotBlank(), "Ingresa el nombre.")
        val u = local.usuario(local.sesion().correo).copy(nombre = nombre.trim(), telefono = telefono)
        local.db.usuarioDao().guardar(u)
        u.cuenta()
    }
    open suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> {
        val s = try { local.sesion(permitirTemporal = true) } catch (e: ReglaLocal) { return Resultado.Fallo(e.error) }
        val resultado = local.ejecutar {
            if (s.rol == Rol.CONDUCTOR) falla(ErrorApp.SinPermiso())
            validar(Validaciones.cambioPassword(passwordActual, passwordNueva, passwordNueva))
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
    open suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val orden = Collator.getInstance(Locale.forLanguageTag("es-CL"))
            .apply { strength = Collator.PRIMARY }
        local.db.usuarioDao().listarSolicitantesPendientes().sortedWith { a, b ->
            orden.compare(a.nombre, b.nombre).takeIf { it != 0 } ?: a.correo.compareTo(b.correo)
        }.map { it.solicitante() }
    }
    private suspend fun aprobar(correo: String, estado: EstadoSolicitante): Resultado<Solicitante> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val u = local.usuario(Validaciones.correo(correo))
        if (u.rol != "SOLICITANTE") falla(ErrorApp.NoEncontrado)
        exigir(u.estadoSolicitante == "PENDIENTE", "La cuenta ya fue revisada.")
        val nueva = u.copy(estadoSolicitante = estado.name)
        local.db.usuarioDao().guardar(nueva)
        nueva.solicitante()
    }
    open suspend fun aprobarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.APROBADO)
    open suspend fun rechazarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.RECHAZADO)
    open suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = local.ejecutar {
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
