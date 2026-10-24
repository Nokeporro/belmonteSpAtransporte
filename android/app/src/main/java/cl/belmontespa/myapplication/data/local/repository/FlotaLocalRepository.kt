package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.local.entity.*
import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Validaciones
import java.security.SecureRandom
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class FlotaLocalRepository @Inject constructor(private val local: SoporteLocal) : FlotaRepository {
    private val random = SecureRandom()
    override suspend fun listarConductores(): Resultado<List<Conductor>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        local.db.usuarioDao().observarPorRol("CONDUCTOR").first().map { local.conductor(it.correo) }
    }
    override suspend fun obtenerConductor(correo: String): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        local.conductor(correo)
    }
    override suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        validar(Validaciones.conductor(cuenta))
        val correo = Validaciones.correo(cuenta.correo)
        if (local.db.usuarioDao().obtener(correo) != null) falla(ErrorApp.Conflicto("El correo ya está registrado."))
        val u = UsuarioEntity(correo, cuenta.nombre.trim(), Validaciones.telefono(cuenta.telefono),
            "CONDUCTOR", true, "DISPONIBLE", null, null, null, FechaIso.texto(local.ahora()), Hashes.password(cuenta.password))
        local.db.usuarioDao().insertar(u)
        local.conductor(u.correo)
    }
    override suspend fun actualizarConductor(correo: String, nombre: String, telefono: String?, password: String?): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val correoNormalizado = Validaciones.correo(correo)
        validar(Validaciones.conductor(NuevaCuenta(nombre, correoNormalizado, telefono, password.orEmpty()), passwordRequerida = false))
        val u = local.usuario(correoNormalizado)
        exigir(u.rol == "CONDUCTOR", "La cuenta no es de un conductor.")
        local.db.usuarioDao().guardar(u.copy(nombre = nombre.trim(), telefono = Validaciones.telefono(telefono),
            passwordSha256 = password?.takeIf { it.isNotEmpty() }?.let(Hashes::password) ?: u.passwordSha256))
        local.conductor(correoNormalizado)
    }
    override suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val u = local.usuario(Validaciones.correo(correo))
        if (u.rol != "CONDUCTOR") falla(ErrorApp.NoEncontrado)
        if (u.activo != activo) {
            exigir(activo || local.viajes().none { it.correoConductor == u.correo && it.estado in listOf("ASIGNADO", "EN_CURSO") }, "El conductor tiene viajes asignados o en curso.")
            local.db.usuarioDao().guardar(u.copy(activo = activo))
        }
        local.conductor(u.correo)
    }
    override suspend fun listarMoviles(): Resultado<List<Movil>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        local.db.movilDao().observarTodos().first().map { it.modelo() }
    }
    override suspend fun obtenerMovil(patente: String): Resultado<Movil> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        (local.db.movilDao().obtener(patente) ?: falla(ErrorApp.NoEncontrado)).modelo()
    }
    override suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        validar(Validaciones.movil(movil))
        val patente = Validaciones.patente(movil.patente)
        if (local.db.movilDao().obtener(patente) != null) falla(ErrorApp.Conflicto("La patente ya está registrada."))
        val bytes = ByteArray(8).also(random::nextBytes)
        val codigo = bytes.joinToString("") { "%02X".format(it.toInt() and 0xff) }
        val nuevo = MovilEntity(patente, movil.capacidad, movil.gpsSerial, true, "DISPONIBLE",
            codigo)
        local.db.movilDao().insertar(nuevo)
        nuevo.modelo()
    }
    override suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val m = local.db.movilDao().obtener(Validaciones.patente(patente)) ?: falla(ErrorApp.NoEncontrado)
        if (m.activo == activo) return@ejecutar m.modelo()
        exigir(activo || local.viajes().none { it.patenteMovil == m.patente && it.estado in listOf("ASIGNADO", "EN_CURSO") }, "El vehículo tiene viajes asignados o en curso.")
        val nuevo = m.copy(activo = activo)
        local.db.movilDao().guardar(nuevo)
        nuevo.modelo()
    }
    override suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val p = patente?.takeIf { it.isNotBlank() }?.let(Validaciones::patente)
        val c = correo?.takeIf { it.isNotBlank() }?.let(Validaciones::correo)
        validar(buildMap {
            if (p != null && !p.matches(Regex("[A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2}"))) put("patente", "Ingresa una patente válida.")
            if (c != null && (c.length > 150 || !c.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))) put("correo", "Ingresa un correo válido.")
        })
        val moviles = local.db.movilDao().observarTodos().first().filter { p == null || it.patente == p }
        moviles.flatMap { m -> local.db.habilitacionDao().observarDelMovil(m.patente).first()
            .filter { c == null || it.correoConductor == c }.map { h ->
                val u = local.usuario(h.correoConductor)
                Habilitacion(m.patente, m.capacidad, EstadoMovil.valueOf(m.estadoMovil), m.activo,
                    u.correo, u.nombre, EstadoConductor.valueOf(u.estadoConductor!!), u.activo)
            }
        }.sortedWith(compareBy<Habilitacion> { it.patente }.thenBy { it.nombreConductor }.thenBy { it.correoConductor })
    }
    override suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val p = Validaciones.patente(patente)
        val c = Validaciones.correo(correoConductor)
        val m = local.db.movilDao().obtener(p) ?: falla(ErrorApp.NoEncontrado)
        val u = local.usuario(c)
        if (u.rol != "CONDUCTOR") falla(ErrorApp.NoEncontrado)
        val existente = local.db.habilitacionDao().observarDelMovil(p).first().any { it.correoConductor == c }
        if (!existente) {
            exigir(m.activo && u.activo, "El vehículo y el conductor deben estar activos.")
            local.db.habilitacionDao().insertar(HabilitacionEntity(p, c))
        }
        Habilitacion(m.patente, m.capacidad, EstadoMovil.valueOf(m.estadoMovil), m.activo,
            u.correo, u.nombre, EstadoConductor.valueOf(u.estadoConductor!!), u.activo)
    }
}
