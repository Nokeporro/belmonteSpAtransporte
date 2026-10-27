package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.model.Cuenta
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
import java.util.Locale

/**
 * Cuenta propia (la de la sesión), aprobación de Solicitantes y alta de Administradores.
 *
 * Los perfiles y formularios de T06 leen la cuenta real de la sesión demo.
 */
open class CuentaRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    open suspend fun obtenerPerfil(): Resultado<Cuenta> = local.ejecutar {
        val sesion = local.sesion(permitirTemporal = true).obtener { return@ejecutar it }
        val usuario = local.buscarUsuario(sesion.correo) ?: return@ejecutar noEncontrado
        Resultado.Exito(aCuenta(usuario))
    }
    open suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = local.ejecutar {
        if (nombre.isBlank()) return@ejecutar conflicto("Ingresa el nombre.")
        val sesion = local.sesion().obtener { return@ejecutar it }
        val u = (local.buscarUsuario(sesion.correo) ?: return@ejecutar noEncontrado).copy(nombre = nombre.trim(), telefono = telefono)
        local.db.usuarioDao().guardar(u)
        Resultado.Exito(aCuenta(u))
    }
    open suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> {
        val s = local.sesion(permitirTemporal = true).obtener { return it }
        val resultado = local.ejecutar {
            if (s.rol == Rol.CONDUCTOR) return@ejecutar sinPermiso()
            val errores = Validaciones.cambioPassword(passwordActual, passwordNueva, passwordNueva)
            if (errores.isNotEmpty()) return@ejecutar invalido(errores)
            val u = local.buscarUsuario(s.correo) ?: return@ejecutar noEncontrado
            val normal = u.passwordSha256 == Hashes.password(passwordActual)
            val temporal = u.passwordTemporalSha256 == Hashes.password(passwordActual) &&
                (u.passwordTemporalExpira ?: Long.MIN_VALUE) > local.clock.millis()
            if (!normal && !temporal) return@ejecutar invalido(mapOf("passwordActual" to "No coincide."))
            local.db.usuarioDao().guardar(u.copy(passwordSha256 = Hashes.password(passwordNueva),
                passwordTemporalSha256 = null, passwordTemporalExpira = null))
            Resultado.Exito(Unit)
        }
        // Solo sale del flujo obligatorio después de persistir el cambio.
        if (resultado is Resultado.Exito) local.sesiones.guardar(s)
        return resultado
    }
    open suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val orden = Collator.getInstance(Locale.forLanguageTag("es-CL"))
            .apply { strength = Collator.PRIMARY }
        Resultado.Exito(
            local.db.usuarioDao().listarSolicitantesPendientes().sortedWith { a, b ->
                orden.compare(a.nombre, b.nombre).takeIf { it != 0 } ?: a.correo.compareTo(b.correo)
            }.map(::aSolicitante),
        )
    }
    private suspend fun aprobar(correo: String, estado: EstadoSolicitante): Resultado<Solicitante> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val u = local.buscarUsuario(Validaciones.correo(correo)) ?: return@ejecutar noEncontrado
        if (u.rol != "SOLICITANTE") return@ejecutar noEncontrado
        if (u.estadoSolicitante != "PENDIENTE") return@ejecutar conflicto("La cuenta ya fue revisada.")
        val nueva = u.copy(estadoSolicitante = estado.name)
        local.db.usuarioDao().guardar(nueva)
        Resultado.Exito(aSolicitante(nueva))
    }
    open suspend fun aprobarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.APROBADO)
    open suspend fun rechazarSolicitante(correo: String) = aprobar(correo, EstadoSolicitante.RECHAZADO)
    open suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val errores = Validaciones.administrador(cuenta)
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val correo = Validaciones.correo(cuenta.correo)
        if (local.db.usuarioDao().obtener(correo) != null) return@ejecutar conflicto("El correo ya está registrado.")
        val u = UsuarioEntity(correo, cuenta.nombre.trim(), Validaciones.telefono(cuenta.telefono),
            "ADMINISTRADOR", true, null, null, null, null, FechaIso.texto(local.ahora()), null,
            Hashes.password(cuenta.password), local.clock.millis() + VIGENCIA_TEMPORAL.toMillis())
        local.db.usuarioDao().insertar(u)
        Resultado.Exito(aCuenta(u))
    }
}
