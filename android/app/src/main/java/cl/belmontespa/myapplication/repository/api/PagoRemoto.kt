package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.PagoEstadoRequest
import cl.belmontespa.myapplication.repository.dto.PagoRequest
import cl.belmontespa.myapplication.repository.dto.aModelo

internal class PagoRemoto(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)
    private val cache = CacheViajes(local)

    suspend fun obtenerPago(hash: String): Resultado<Pago> = llamar { api.pago(hash).aModelo() }

    suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = llamar {
        val respuesta = api.registrarPago(
            PagoRequest(pago.folio, pago.monto, pago.codSbif, pago.medioPago.name, pago.tipoRecibo.name),
        )
        cache.refrescar(respuesta.folio)
        respuesta.aModelo()
    }

    suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> = llamar {
        val respuesta = api.cambiarEstadoPago(hash, PagoEstadoRequest(estado.name))
        cache.refrescar(respuesta.folio)
        respuesta.aModelo()
    }
}
