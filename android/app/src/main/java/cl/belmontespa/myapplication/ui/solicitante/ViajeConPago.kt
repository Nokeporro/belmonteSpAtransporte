package cl.belmontespa.myapplication.ui.solicitante

import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.valor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/**
 * El viaje con su pago, que la cancelación necesita para mostrar el efecto (HU16). Si el pago no se puede leer, el
 * flujo termina con ese error y el viaje no se publica.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun ViajeRepository.observarConPago(folio: String, pagos: PagoRepository): Flow<Pair<Viaje?, Pago?>> =
    observarViaje(folio).mapLatest { viaje -> viaje to viaje?.hashPago?.let { hash -> pagos.obtenerPago(hash).valor() } }
