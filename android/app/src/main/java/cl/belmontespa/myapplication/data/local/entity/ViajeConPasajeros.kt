package cl.belmontespa.myapplication.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Un viaje con sus pasajeros y la marca de abordaje de cada uno. */
data class ViajeConPasajeros(
    @Embedded val viaje: ViajeEntity,
    @Relation(entity = ViajePasajeroEntity::class, parentColumn = "folio", entityColumn = "folio")
    val pasajeros: List<PasajeroDeViaje>,
)

/** Fila de `viaje_pasajeros` junto al nombre del pasajero. */
data class PasajeroDeViaje(
    @Embedded val detalle: ViajePasajeroEntity,
    @Relation(parentColumn = "dni_hash", entityColumn = "dni_hash")
    val pasajero: PasajeroEntity,
)
