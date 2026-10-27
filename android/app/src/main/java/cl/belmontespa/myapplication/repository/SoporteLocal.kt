package cl.belmontespa.myapplication.repository

import android.app.Application
import androidx.room.withTransaction
import cl.belmontespa.myapplication.database.AppDatabase
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.MovilEntity
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.PagoEntity
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.model.UsuarioEntity
import cl.belmontespa.myapplication.model.ViajeEntity
import cl.belmontespa.myapplication.session.SesionStore
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

// Infraestructura compartida de los repositorios demo; no se accede a Room desde la UI.
open class SoporteLocal(
    val db: AppDatabase,
    val sesiones: SesionStore,
    val clock: Clock,
) {
    fun ahora(): LocalDateTime = LocalDateTime.now(clock)

    companion object {
        /** La base y la sesión únicas de la app, con el reloj de Chile. */
        fun de(application: Application): SoporteLocal =
            SoporteLocal(AppDatabase.getDatabase(application), SesionStore.getInstance(application), Reloj.santiago)
    }

    suspend fun sesion(rol: Rol? = null, permitirTemporal: Boolean = false): Resultado<Sesion> {
        val sesion = when (val estado = sesiones.estado.first()) {
            is EstadoSesion.Autenticada -> estado.sesion
            is EstadoSesion.Temporal -> if (permitirTemporal) estado.sesion else return sinPermiso()
            else -> return Resultado.Fallo(ErrorApp.SesionExpirada)
        }
        if (rol != null && sesion.rol != rol) return sinPermiso()
        return Resultado.Exito(sesion)
    }

    /**
     * Corre [bloque] en una transacción de Room. Si devuelve un fallo, deshace lo que ya haya escrito: sale de la
     * transacción con [Deshacer], que se atrapa aquí mismo y entrega ese fallo.
     */
    suspend fun <T> ejecutar(bloque: suspend () -> Resultado<T>): Resultado<T> = try {
        db.withTransaction {
            when (val resultado = bloque()) {
                is Resultado.Exito -> resultado
                is Resultado.Fallo -> throw Deshacer(resultado)
                is Resultado.Encolado -> throw Deshacer(resultado)
            }
        }
    } catch (e: CancellationException) { throw e
    } catch (e: Deshacer) { e.resultado
    } catch (e: android.database.sqlite.SQLiteConstraintException) {
        Resultado.Fallo(ErrorApp.Conflicto("El registro ya existe o está relacionado con otros datos."))
    } catch (e: IllegalArgumentException) {
        Resultado.Fallo(ErrorApp.Validacion(mapOf("datos" to "Revisa los datos ingresados.")))
    } catch (e: Exception) { Resultado.Fallo(ErrorApp.Inesperado) }

    suspend fun viajes(): List<ViajeEntity> = db.viajeDao().observarLista(null, null, null, null, false).first().map { it.viaje }
    suspend fun buscarViaje(folio: String): ViajeEntity? = db.viajeDao().obtener(folio)
    suspend fun buscarUsuario(correo: String): UsuarioEntity? = db.usuarioDao().obtener(correo)

    /** El conductor con ese correo; `null` si no existe o la cuenta es de otro rol. */
    suspend fun conductor(correo: String): Conductor? =
        buscarUsuario(correo)?.takeIf { it.rol == Rol.CONDUCTOR.name }?.let { conductorDe(it) }

    suspend fun conductorDe(usuario: UsuarioEntity): Conductor {
        val agenda = viajes().filter { it.correoConductor == usuario.correo }
        return Conductor(usuario.nombre, usuario.correo, usuario.telefono, FechaIso.leer(usuario.fechaRegistro), usuario.activo,
            EstadoConductor.valueOf(usuario.estadoConductor!!), agenda.count { it.estado == "ASIGNADO" },
            agenda.firstOrNull { it.estado == "EN_CURSO" }?.folio)
    }

    /** El viaje, solo si está dentro del alcance de la sesión (el Solicitante los suyos, el Conductor los asignados). */
    suspend fun alcance(folio: String): Resultado<ViajeEntity> {
        val sesion = sesion().obtener { return it }
        val viaje = buscarViaje(folio) ?: return noEncontrado
        val ajeno = sesion.rol == Rol.SOLICITANTE && viaje.correoSolicitante != sesion.correo ||
            sesion.rol == Rol.CONDUCTOR && viaje.correoConductor != sesion.correo
        return if (ajeno) sinPermiso() else Resultado.Exito(viaje)
    }
}

/** Sale de la transacción de [SoporteLocal.ejecutar] para deshacerla; lleva el fallo que se devuelve. */
private class Deshacer(val resultado: Resultado<Nothing>) : RuntimeException()

/** Vigencia de toda contraseña temporal: la de RF21 y la inicial de un Administrador nuevo (CU-18). */
internal val VIGENCIA_TEMPORAL: Duration = Duration.ofHours(24)

// Los fallos que una operación devuelve con `return@ejecutar`.
internal fun conflicto(mensaje: String) = Resultado.Fallo(ErrorApp.Conflicto(mensaje))
internal fun invalido(errores: Map<String, String>) = Resultado.Fallo(ErrorApp.Validacion(errores))
internal fun sinPermiso() = Resultado.Fallo(ErrorApp.SinPermiso())
internal val noEncontrado = Resultado.Fallo(ErrorApp.NoEncontrado)

/**
 * El valor de un resultado exitoso; si no lo es, ejecuta [salir] con el fallo, que corta la operación:
 * `val sesion = local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }`.
 */
internal inline fun <T> Resultado<T>.obtener(salir: (Resultado<Nothing>) -> Nothing): T = when (this) {
    is Resultado.Exito -> valor
    is Resultado.Fallo -> salir(this)
    is Resultado.Encolado -> salir(this)
}
internal fun aCuenta(u: UsuarioEntity) = Cuenta(u.nombre, u.correo, u.telefono, Rol.valueOf(u.rol),
    FechaIso.leer(u.fechaRegistro), u.razonSocial, u.rutEmpresa, u.estadoSolicitante?.let(EstadoSolicitante::valueOf))
internal fun aSolicitante(u: UsuarioEntity) = Solicitante(u.nombre, u.correo, u.telefono, FechaIso.leer(u.fechaRegistro),
    u.razonSocial!!, u.rutEmpresa!!, EstadoSolicitante.valueOf(u.estadoSolicitante!!))
internal fun aMovil(m: MovilEntity) = Movil(m.patente, m.capacidad, m.gpsSerial, m.activo, EstadoMovil.valueOf(m.estadoMovil), m.codigoAbordaje)
internal fun aPago(p: PagoEntity) = Pago(p.hash, p.folio, p.monto, EstadoPago.valueOf(p.estadoPago), p.banco, p.codSbif,
    p.rutEmpresa, MedioPago.valueOf(p.medioPago), TipoRecibo.valueOf(p.tipoRecibo), p.fechaPago?.let(FechaIso::leer), p.montoReembolsado)
