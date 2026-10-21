package cl.belmontespa.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.belmontespa.myapplication.data.local.entity.ViajePasajeroEntity

@Dao
interface ViajePasajeroDao {
    /** Una asociación existente no se duplica: devuelve -1 en ese caso. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(detalle: ViajePasajeroEntity): Long

    @Query("SELECT * FROM viaje_pasajeros WHERE folio = :folio")
    suspend fun obtenerDelViaje(folio: String): List<ViajePasajeroEntity>

    /** Marca el abordaje solo si estaba Por abordar: repetir no cambia la hora guardada. */
    @Query(
        "UPDATE viaje_pasajeros SET fh_abordaje = :fhAbordaje " +
            "WHERE folio = :folio AND dni_hash = :dniHash AND fh_abordaje IS NULL",
    )
    suspend fun marcarAbordaje(folio: String, dniHash: String, fhAbordaje: String): Int

    /** Todos los pasajeros del viaje vuelven a Por abordar (V2 con cambio de vehículo y V8). */
    @Query("UPDATE viaje_pasajeros SET fh_abordaje = NULL WHERE folio = :folio")
    suspend fun limpiarAbordaje(folio: String)
}
