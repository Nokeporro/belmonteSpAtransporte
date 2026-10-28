package cl.belmontespa.myapplication.viewmodel.solicitante

import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.repository.PagoRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
internal fun ViajeRepository.observarConPago(folio: String, pagos: PagoRepository): Flow<Pair<Viaje?, Pago?>> =
    observarViaje(folio).mapLatest { viaje -> viaje to viaje?.hashPago?.let { hash -> pagos.obtenerPago(hash).valor() } }
