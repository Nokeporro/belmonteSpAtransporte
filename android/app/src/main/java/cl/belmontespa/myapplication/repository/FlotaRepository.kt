package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.HabilitacionEntity
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.MovilEntity
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.UsuarioEntity
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Hashes
import cl.belmontespa.myapplication.util.Validaciones
import java.security.SecureRandom
import kotlinx.coroutines.flow.first

/** Conductores, vehículos y habilitaciones (Administrador). */
open class FlotaRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    private val random = SecureRandom()
    open suspend fun listarConductores(): Resultado<List<Conductor>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        Resultado.Exito(local.db.usuarioDao().observarPorRol("CONDUCTOR").first().map { local.conductorDe(it) })
    }
    open suspend fun obtenerConductor(correo: String): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val conductor = local.conductor(correo) ?: return@ejecutar noEncontrado
        Resultado.Exito(conductor)
    }
    open suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val errores = Validaciones.conductor(cuenta)
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val correo = Validaciones.correo(cuenta.correo)
        if (local.db.usuarioDao().obtener(correo) != null) return@ejecutar conflicto("El correo ya está registrado.")
        val u = UsuarioEntity(correo, cuenta.nombre.trim(), Validaciones.telefono(cuenta.telefono),
            "CONDUCTOR", true, "DISPONIBLE", null, null, null, FechaIso.texto(local.ahora()), Hashes.password(cuenta.password))
        local.db.usuarioDao().insertar(u)
        Resultado.Exito(local.conductorDe(u))
    }
    /** Sin `password` se conserva la actual. */
    open suspend fun actualizarConductor(correo: String, nombre: String, telefono: String?, password: String?): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val correoNormalizado = Validaciones.correo(correo)
        val errores = Validaciones.conductor(NuevaCuenta(nombre, correoNormalizado, telefono, password.orEmpty()), passwordRequerida = false)
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val u = local.buscarUsuario(correoNormalizado) ?: return@ejecutar noEncontrado
        if (u.rol != "CONDUCTOR") return@ejecutar conflicto("La cuenta no es de un conductor.")
        local.db.usuarioDao().guardar(u.copy(nombre = nombre.trim(), telefono = Validaciones.telefono(telefono),
            passwordSha256 = password?.takeIf { it.isNotEmpty() }?.let(Hashes::password) ?: u.passwordSha256))
        val conductor = local.conductor(correoNormalizado) ?: return@ejecutar noEncontrado
        Resultado.Exito(conductor)
    }
    open suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val u = local.buscarUsuario(Validaciones.correo(correo)) ?: return@ejecutar noEncontrado
        if (u.rol != "CONDUCTOR") return@ejecutar noEncontrado
        if (u.activo != activo) {
            val conViajes = local.viajes().any { it.correoConductor == u.correo && it.estado in listOf("ASIGNADO", "EN_CURSO") }
            if (!activo && conViajes) return@ejecutar conflicto("El conductor tiene viajes asignados o en curso.")
            local.db.usuarioDao().guardar(u.copy(activo = activo))
        }
        val conductor = local.conductor(u.correo) ?: return@ejecutar noEncontrado
        Resultado.Exito(conductor)
    }
    open suspend fun listarMoviles(): Resultado<List<Movil>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        Resultado.Exito(local.db.movilDao().observarTodos().first().map(::aMovil))
    }
    open suspend fun obtenerMovil(patente: String): Resultado<Movil> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val movil = local.db.movilDao().obtener(patente) ?: return@ejecutar noEncontrado
        Resultado.Exito(aMovil(movil))
    }
    open suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val errores = Validaciones.movil(movil)
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val patente = Validaciones.patente(movil.patente)
        if (local.db.movilDao().obtener(patente) != null) return@ejecutar conflicto("La patente ya está registrada.")
        val bytes = ByteArray(8).also(random::nextBytes)
        val codigo = bytes.joinToString("") { "%02X".format(it.toInt() and 0xff) }
        val nuevo = MovilEntity(patente, movil.capacidad, movil.gpsSerial, true, "DISPONIBLE",
            codigo)
        local.db.movilDao().insertar(nuevo)
        Resultado.Exito(aMovil(nuevo))
    }
    open suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val m = local.db.movilDao().obtener(Validaciones.patente(patente)) ?: return@ejecutar noEncontrado
        if (m.activo == activo) return@ejecutar Resultado.Exito(aMovil(m))
        val conViajes = local.viajes().any { it.patenteMovil == m.patente && it.estado in listOf("ASIGNADO", "EN_CURSO") }
        if (!activo && conViajes) return@ejecutar conflicto("El vehículo tiene viajes asignados o en curso.")
        val nuevo = m.copy(activo = activo)
        local.db.movilDao().guardar(nuevo)
        Resultado.Exito(aMovil(nuevo))
    }
    /** Todas, o filtradas por vehículo (`patente`) o por conductor (`correo`). */
    open suspend fun listarHabilitaciones(patente: String? = null, correo: String? = null): Resultado<List<Habilitacion>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val p = patente?.takeIf { it.isNotBlank() }?.let(Validaciones::patente)
        val c = correo?.takeIf { it.isNotBlank() }?.let(Validaciones::correo)
        val errores = buildMap {
            if (p != null && !p.matches(Regex("[A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2}"))) put("patente", "Ingresa una patente válida.")
            if (c != null && (c.length > 150 || !c.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))) put("correo", "Ingresa un correo válido.")
        }
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val moviles = local.db.movilDao().observarTodos().first().filter { p == null || it.patente == p }
        val habilitaciones = moviles.flatMap { m -> local.db.habilitacionDao().observarDelMovil(m.patente).first()
            .filter { c == null || it.correoConductor == c }.mapNotNull { h ->
                local.buscarUsuario(h.correoConductor)?.let { habilitacion(m, it) }
            }
        }
        Resultado.Exito(habilitaciones.sortedWith(compareBy<Habilitacion> { it.patente }.thenBy { it.nombreConductor }.thenBy { it.correoConductor }))
    }
    open suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val p = Validaciones.patente(patente)
        val c = Validaciones.correo(correoConductor)
        val m = local.db.movilDao().obtener(p) ?: return@ejecutar noEncontrado
        val u = local.buscarUsuario(c) ?: return@ejecutar noEncontrado
        if (u.rol != "CONDUCTOR") return@ejecutar noEncontrado
        val existente = local.db.habilitacionDao().observarDelMovil(p).first().any { it.correoConductor == c }
        if (!existente) {
            if (!m.activo || !u.activo) return@ejecutar conflicto("El vehículo y el conductor deben estar activos.")
            local.db.habilitacionDao().insertar(HabilitacionEntity(p, c))
        }
        Resultado.Exito(habilitacion(m, u))
    }

    private fun habilitacion(movil: MovilEntity, conductor: UsuarioEntity) =
        Habilitacion(movil.patente, movil.capacidad, EstadoMovil.valueOf(movil.estadoMovil), movil.activo,
            conductor.correo, conductor.nombre, EstadoConductor.valueOf(conductor.estadoConductor!!), conductor.activo)
}
