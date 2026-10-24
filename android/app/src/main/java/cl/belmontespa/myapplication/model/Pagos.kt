package cl.belmontespa.myapplication.model

import java.time.LocalDateTime

data class Pago(
    val hash: String,
    val folio: String,
    val monto: Int,
    val estado: EstadoPago,
    val banco: String,
    val codSbif: Int,
    val rutEmpresa: String,
    val medioPago: MedioPago,
    val tipoRecibo: TipoRecibo,
    val fechaPago: LocalDateTime?,
    val montoReembolsado: Int?,
)

data class NuevoPago(
    val folio: String,
    val monto: Int,
    val codSbif: Int,
    val medioPago: MedioPago,
    val tipoRecibo: TipoRecibo,
)

/** P2/P3 manuales. RECHAZADO, ANULADO y REEMBOLSADO no admiten cambios. */
fun EstadoPago.permiteCambioA(destino: EstadoPago): Boolean =
    this == EstadoPago.PENDIENTE && destino in listOf(EstadoPago.PAGADO, EstadoPago.RECHAZADO, EstadoPago.ANULADO) ||
        this == EstadoPago.PAGADO && destino == EstadoPago.ANULADO

data class Banco(val nombre: String, val codSbif: Int)

data class Region(val nombre: String, val comunas: List<String>)

/** Catálogos de captura de `GET /catalogos`, ordenados por nombre. */
data class Catalogos(
    val bancos: List<Banco>,
    val mediosPago: List<MedioPago>,
    val tiposRecibo: List<TipoRecibo>,
    val regiones: List<Region>,
)
