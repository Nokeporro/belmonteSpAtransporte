package cl.belmontespa.myapplication.data.repository

import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado

/** Pago de un viaje (P1 a P3); P4 y P5 los aplica la cancelación del viaje. */
interface PagoRepository {
    suspend fun obtenerPago(hash: String): Resultado<Pago>

    suspend fun registrarPago(pago: NuevoPago): Resultado<Pago>

    suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago>
}
