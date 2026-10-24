package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.DatosDemo
import cl.belmontespa.myapplication.data.local.entity.PagoEntity
import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import java.util.UUID
import javax.inject.Inject

class PagoLocalRepository @Inject constructor(private val local: SoporteLocal) : PagoRepository {
    override suspend fun obtenerPago(hash: String): Resultado<Pago> = local.ejecutar {
        val p = local.db.pagoDao().obtener(hash) ?: falla(ErrorApp.NoEncontrado)
        val s = local.sesion()
        exigir(s.rol != Rol.CONDUCTOR, "No tienes permiso para consultar pagos.")
        local.alcance(p.folio)
        p.modelo()
    }
    override suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = local.ejecutar {
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
    override suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val p = local.db.pagoDao().obtener(hash) ?: falla(ErrorApp.NoEncontrado)
        exigir(p.estadoPago == "PENDIENTE" && estado in listOf(EstadoPago.PAGADO, EstadoPago.RECHAZADO, EstadoPago.ANULADO) ||
            p.estadoPago == "PAGADO" && estado == EstadoPago.ANULADO, "El cambio de pago no está permitido.")
        val nuevo = p.copy(estadoPago = estado.name, fechaPago = if (estado == EstadoPago.PAGADO) FechaIso.texto(local.ahora()) else p.fechaPago)
        local.db.pagoDao().guardar(nuevo)
        val v = local.viaje(p.folio)
        local.db.viajeDao().guardar(v.copy(estadoPago = estado.name,
            estado = if (estado == EstadoPago.ANULADO && v.estado in listOf("PENDIENTE", "ASIGNADO")) "CANCELADO" else v.estado))
        nuevo.modelo()
    }
}
