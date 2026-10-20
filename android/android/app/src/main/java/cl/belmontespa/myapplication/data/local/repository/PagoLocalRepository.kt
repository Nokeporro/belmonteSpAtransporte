package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import javax.inject.Inject

/** Pagos de la build demo sobre Room (P1 a P3). Cada operación la implementa la HU que se indica. */
class PagoLocalRepository @Inject constructor() : PagoRepository {

    override suspend fun obtenerPago(hash: String): Resultado<Pago> = TODO("HU20 y HU21")

    override suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = TODO("HU19: P1")

    override suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> =
        TODO("HU20: P2, P3 y V6")
}
