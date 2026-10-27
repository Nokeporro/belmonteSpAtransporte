package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.database.DatosDemo
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.PagoEntity
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.permiteCambioA
import cl.belmontespa.myapplication.util.FechaIso
import java.util.UUID

/** Pago de un viaje (P1 a P3); P4 y P5 los aplica la cancelación del viaje. */
open class PagoRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    open suspend fun obtenerPago(hash: String): Resultado<Pago> = local.ejecutar {
        val s = local.sesion().obtener { return@ejecutar it }
        if (s.rol == Rol.CONDUCTOR) return@ejecutar sinPermiso()
        val p = local.db.pagoDao().obtener(hash) ?: return@ejecutar noEncontrado
        local.alcance(p.folio).obtener { return@ejecutar it }
        Resultado.Exito(aPago(p))
    }
    open suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val v = local.buscarViaje(pago.folio) ?: return@ejecutar noEncontrado
        if (v.estado == "CANCELADO" || v.hashPago != null) return@ejecutar conflicto("El viaje ya tiene un pago o está cancelado.")
        if (pago.monto <= 0) return@ejecutar invalido(mapOf("monto" to "Ingresa un monto entero mayor que cero."))
        val banco = DatosDemo.bancos.find { it.codSbif == pago.codSbif }
            ?: return@ejecutar invalido(mapOf("banco" to "Selecciona un banco del catálogo."))
        val u = local.buscarUsuario(v.correoSolicitante) ?: return@ejecutar noEncontrado
        val p = PagoEntity(UUID.randomUUID().toString().replace("-", ""), v.folio, pago.monto, "PENDIENTE", banco.nombre,
            banco.codSbif, u.rutEmpresa!!, pago.medioPago.name, pago.tipoRecibo.name, null, null)
        local.db.pagoDao().insertar(p)
        local.db.viajeDao().guardar(v.copy(hashPago = p.hash, estadoPago = p.estadoPago))
        Resultado.Exito(aPago(p))
    }
    open suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val p = local.db.pagoDao().obtener(hash) ?: return@ejecutar noEncontrado
        if (!EstadoPago.valueOf(p.estadoPago).permiteCambioA(estado)) return@ejecutar conflicto("El cambio de pago no está permitido.")
        val nuevo = p.copy(estadoPago = estado.name, fechaPago = if (estado == EstadoPago.PAGADO) FechaIso.texto(local.ahora()) else p.fechaPago)
        local.db.pagoDao().guardar(nuevo)
        val v = local.buscarViaje(p.folio) ?: return@ejecutar noEncontrado
        local.db.viajeDao().guardar(v.copy(estadoPago = estado.name,
            estado = if (estado == EstadoPago.ANULADO && v.estado in listOf("PENDIENTE", "ASIGNADO")) "CANCELADO" else v.estado))
        Resultado.Exito(aPago(nuevo))
    }
}
