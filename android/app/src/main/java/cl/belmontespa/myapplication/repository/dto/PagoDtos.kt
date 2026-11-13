package cl.belmontespa.myapplication.repository.dto

import cl.belmontespa.myapplication.model.Banco
import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.TipoRecibo
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class PagoRequest(val folio: String, val monto: Int, val codSbif: Int, val medioPago: String, val tipoRecibo: String)

@Serializable
data class PagoEstadoRequest(val estado: String)

@Serializable
data class PagoResponse(
    val hash: String,
    val folio: String,
    val monto: Int,
    val estadoPago: String,
    val banco: String,
    val codSbif: Int,
    val rutEmpresa: String,
    val medioPago: String,
    val tipoRecibo: String,
    @Serializable(with = SerializadorFecha::class) val fechaPago: LocalDateTime?,
    val montoReembolsado: Int?,
)

@Serializable
data class BancoDto(val nombre: String, val codSbif: Int)

@Serializable
data class RegionDto(val nombre: String, val comunas: List<String>)

@Serializable
data class CatalogosResponse(
    val bancos: List<BancoDto>,
    val mediosPago: List<String>,
    val tiposRecibo: List<String>,
    val regiones: List<RegionDto>,
)

fun PagoResponse.aModelo() = Pago(
    hash,
    folio,
    monto,
    EstadoPago.valueOf(estadoPago),
    banco,
    codSbif,
    rutEmpresa,
    MedioPago.valueOf(medioPago),
    TipoRecibo.valueOf(tipoRecibo),
    fechaPago,
    montoReembolsado,
)

fun CatalogosResponse.aModelo() = Catalogos(
    bancos = bancos.map { Banco(it.nombre, it.codSbif) },
    mediosPago = mediosPago.mapNotNull { nombre -> MedioPago.entries.find { it.name == nombre } },
    tiposRecibo = tiposRecibo.mapNotNull { nombre -> TipoRecibo.entries.find { it.name == nombre } },
    regiones = regiones.map { Region(it.nombre, it.comunas) },
)
