package cl.belmontespa.myapplication.data.remote.repository

import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import javax.inject.Inject

/** Implementación remota; hasta T14 responde Sin conexión. */
class PagoRemoteRepository @Inject constructor() : PagoRepository {

    override suspend fun obtenerPago(hash: String): Resultado<Pago> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> =
        Resultado.Fallo(ErrorApp.SinConexion)
}
