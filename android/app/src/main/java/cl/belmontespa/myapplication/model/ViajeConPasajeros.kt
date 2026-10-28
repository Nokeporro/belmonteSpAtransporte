package cl.belmontespa.myapplication.model

import androidx.room.Embedded
import androidx.room.Relation

data class ViajeConPasajeros(
    @Embedded val viaje: ViajeEntity,
    @Relation(entity = ViajePasajeroEntity::class, parentColumn = "folio", entityColumn = "folio")
    val pasajeros: List<PasajeroDeViaje>,
)

data class PasajeroDeViaje(
    @Embedded val detalle: ViajePasajeroEntity,
    @Relation(parentColumn = "dni_hash", entityColumn = "dni_hash")
    val pasajero: PasajeroEntity,
)
