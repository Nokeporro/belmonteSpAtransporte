package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.local.entity.*
import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Validaciones
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class FlotaLocalRepository @Inject constructor(private val local: SoporteLocal) : FlotaRepository {
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
        val u = local.usuario(correo)
        exigir(u.rol == "CONDUCTOR", "La cuenta no es de un conductor.")
        exigir(activo || local.viajes().none { it.correoConductor == correo && it.estado in listOf("ASIGNADO", "EN_CURSO") }, "El conductor tiene viajes asignados o en curso.")
        local.db.usuarioDao().guardar(u.copy(activo = activo))
        local.conductor(correo)
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
        val patente = movil.patente.uppercase().replace("-", "").replace(" ", "")
        exigir(patente.matches(Regex("[A-Z]{4}[0-9]{2}|[A-Z]{2}[0-9]{4}")) && movil.capacidad in 1..99, "Revisa la patente y la capacidad.")
        val nuevo = MovilEntity(patente, movil.capacidad, movil.gpsSerial, true, "DISPONIBLE",
            UUID.randomUUID().toString().replace("-", "").take(16).uppercase())
        local.db.movilDao().insertar(nuevo)
        nuevo.modelo()
    }
    override suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val m = local.db.movilDao().obtener(patente) ?: falla(ErrorApp.NoEncontrado)
        exigir(activo || local.viajes().none { it.patenteMovil == patente && it.estado in listOf("ASIGNADO", "EN_CURSO") }, "El vehículo tiene viajes asignados o en curso.")
        val nuevo = m.copy(activo = activo)
        local.db.movilDao().guardar(nuevo)
        nuevo.modelo()
    }
    override suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val moviles = local.db.movilDao().observarTodos().first().filter { patente == null || it.patente == patente }
        moviles.flatMap { m -> local.db.habilitacionDao().observarDelMovil(m.patente).first()
            .filter { correo == null || it.correoConductor == correo }.map { h ->
                val u = local.usuario(h.correoConductor)
                Habilitacion(m.patente, m.capacidad, EstadoMovil.valueOf(m.estadoMovil), m.activo,
                    u.correo, u.nombre, EstadoConductor.valueOf(u.estadoConductor!!), u.activo)
            }
        }.sortedWith(compareBy<Habilitacion> { it.patente }.thenBy { it.nombreConductor })
    }
    override suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val m = local.db.movilDao().obtener(patente) ?: falla(ErrorApp.NoEncontrado)
        val u = local.usuario(correoConductor)
        exigir(m.activo && u.activo && u.rol == "CONDUCTOR", "El vehículo y el conductor deben estar activos.")
        local.db.habilitacionDao().insertar(HabilitacionEntity(patente, correoConductor))
        Habilitacion(m.patente, m.capacidad, EstadoMovil.valueOf(m.estadoMovil), m.activo,
            u.correo, u.nombre, EstadoConductor.valueOf(u.estadoConductor!!), u.activo)
    }
}
