package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.database.DatosDemo
import cl.belmontespa.myapplication.model.ErrorApp
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
        val s = local.sesion()
        if (s.rol == Rol.CONDUCTOR) falla(ErrorApp.SinPermiso())
        val p = local.db.pagoDao().obtener(hash) ?: falla(ErrorApp.NoEncontrado)
        local.alcance(p.folio)
        p.modelo()
    }
    open suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val v = local.viaje(pago.folio)
        exigir(v.estado != "CANCELADO" && v.hashPago == null, "El viaje ya tiene un pago o está cancelado.")
        validar(if (pago.monto > 0) emptyMap() else mapOf("monto" to "Ingresa un monto entero mayor que cero."))
        val banco = DatosDemo.bancos.find { it.codSbif == pago.codSbif }
            ?: falla(ErrorApp.Validacion(mapOf("banco" to "Selecciona un banco del catálogo.")))
        val u = local.usuario(v.correoSolicitante)
        val p = PagoEntity(UUID.randomUUID().toString().replace("-", ""), v.folio, pago.monto, "PENDIENTE", banco.nombre,
            banco.codSbif, u.rutEmpresa!!, pago.medioPago.name, pago.tipoRecibo.name, null, null)
        local.db.pagoDao().insertar(p)
        local.db.viajeDao().guardar(v.copy(hashPago = p.hash, estadoPago = p.estadoPago))
        p.modelo()
    }
    open suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val p = local.db.pagoDao().obtener(hash) ?: falla(ErrorApp.NoEncontrado)
        exigir(EstadoPago.valueOf(p.estadoPago).permiteCambioA(estado), "El cambio de pago no está permitido.")
        val nuevo = p.copy(estadoPago = estado.name, fechaPago = if (estado == EstadoPago.PAGADO) FechaIso.texto(local.ahora()) else p.fechaPago)
        local.db.pagoDao().guardar(nuevo)
        val v = local.viaje(p.folio)
        local.db.viajeDao().guardar(v.copy(estadoPago = estado.name,
            estado = if (estado == EstadoPago.ANULADO && v.estado in listOf("PENDIENTE", "ASIGNADO")) "CANCELADO" else v.estado))
        nuevo.modelo()
    }
}
