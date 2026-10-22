package cl.belmontespa.myapplication.data.local.repository

import androidx.room.withTransaction
import cl.belmontespa.myapplication.data.local.database.BelmonteDatabase
import cl.belmontespa.myapplication.data.local.entity.*
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

// Infraestructura compartida de los repositorios demo; no se accede a Room desde la UI.
class SoporteLocal @Inject constructor(
    val db: BelmonteDatabase,
    val sesiones: SesionStore,
    val clock: Clock,
) {
    fun ahora(): LocalDateTime = LocalDateTime.now(clock)
    suspend fun sesion(rol: Rol? = null, permitirTemporal: Boolean = false): Sesion {
        val estado = sesiones.estado.first()
        val sesion = when (estado) {
            is EstadoSesion.Autenticada -> estado.sesion
            is EstadoSesion.Temporal -> if (permitirTemporal) estado.sesion else falla(ErrorApp.SinPermiso())
            else -> falla(ErrorApp.SesionExpirada)
        }
        if (rol != null && sesion.rol != rol) falla(ErrorApp.SinPermiso())
        return sesion
    }
    // Todas las escrituras demo se confirman juntas o se revierten.
    suspend fun <T> ejecutar(block: suspend () -> T): Resultado<T> = try {
        Resultado.Exito(db.withTransaction { block() })
    } catch (e: CancellationException) { throw e
    } catch (e: ReglaLocal) { Resultado.Fallo(e.error)
    } catch (e: android.database.sqlite.SQLiteConstraintException) {
        Resultado.Fallo(ErrorApp.Conflicto("El registro ya existe o está relacionado con otros datos."))
    } catch (e: IllegalArgumentException) {
        Resultado.Fallo(ErrorApp.Validacion(mapOf("datos" to "Revisa los datos ingresados.")))
    } catch (e: Exception) { Resultado.Fallo(ErrorApp.Inesperado) }
    suspend fun viajes(): List<ViajeEntity> = db.viajeDao().observarLista(null, null, null, null, false).first().map { it.viaje }
    suspend fun viaje(folio: String): ViajeEntity = db.viajeDao().obtener(folio) ?: falla(ErrorApp.NoEncontrado)
    suspend fun usuario(correo: String): UsuarioEntity = db.usuarioDao().obtener(correo) ?: falla(ErrorApp.NoEncontrado)
    suspend fun conductor(correo: String): Conductor {
        val u = usuario(correo)
        if (u.rol != Rol.CONDUCTOR.name) falla(ErrorApp.NoEncontrado)
        val agenda = viajes().filter { it.correoConductor == correo }
        return Conductor(u.nombre, u.correo, u.telefono, FechaIso.leer(u.fechaRegistro), u.activo,
            EstadoConductor.valueOf(u.estadoConductor!!), agenda.count { it.estado == "ASIGNADO" },
            agenda.firstOrNull { it.estado == "EN_CURSO" }?.folio)
    }
    suspend fun alcance(folio: String): ViajeEntity {
        val s = sesion()
        val v = viaje(folio)
        if (s.rol == Rol.SOLICITANTE && v.correoSolicitante != s.correo ||
            s.rol == Rol.CONDUCTOR && v.correoConductor != s.correo) falla(ErrorApp.SinPermiso())
        return v
    }
}

internal class ReglaLocal(val error: ErrorApp) : RuntimeException()
internal fun validar(errores: Map<String, String>) {
    if (errores.isNotEmpty()) falla(ErrorApp.Validacion(errores))
}
internal fun falla(error: ErrorApp): Nothing = throw ReglaLocal(error)
internal fun exigir(condicion: Boolean, mensaje: String) {
    if (!condicion) falla(ErrorApp.Conflicto(mensaje))
}
internal fun UsuarioEntity.cuenta() = Cuenta(nombre, correo, telefono, Rol.valueOf(rol),
    FechaIso.leer(fechaRegistro), razonSocial, rutEmpresa, estadoSolicitante?.let(EstadoSolicitante::valueOf))
internal fun UsuarioEntity.solicitante() = Solicitante(nombre, correo, telefono, FechaIso.leer(fechaRegistro),
    razonSocial!!, rutEmpresa!!, EstadoSolicitante.valueOf(estadoSolicitante!!))
internal fun MovilEntity.modelo() = Movil(patente, capacidad, gpsSerial, activo, EstadoMovil.valueOf(estadoMovil), codigoAbordaje)
internal fun PagoEntity.modelo() = Pago(hash, folio, monto, EstadoPago.valueOf(estadoPago), banco, codSbif,
    rutEmpresa, MedioPago.valueOf(medioPago), TipoRecibo.valueOf(tipoRecibo), fechaPago?.let(FechaIso::leer), montoReembolsado)
