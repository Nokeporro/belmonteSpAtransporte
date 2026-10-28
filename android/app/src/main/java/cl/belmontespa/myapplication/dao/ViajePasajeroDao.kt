package cl.belmontespa.myapplication.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.belmontespa.myapplication.model.ViajePasajeroEntity

@Dao
interface ViajePasajeroDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(detalle: ViajePasajeroEntity): Long

    @Query("SELECT * FROM viaje_pasajeros WHERE folio = :folio")
    suspend fun obtenerDelViaje(folio: String): List<ViajePasajeroEntity>

    @Query(
        "UPDATE viaje_pasajeros SET fh_abordaje = :fhAbordaje " +
            "WHERE folio = :folio AND dni_hash = :dniHash AND fh_abordaje IS NULL",
    )
    suspend fun marcarAbordaje(folio: String, dniHash: String, fhAbordaje: String): Int

    @Query("UPDATE viaje_pasajeros SET fh_abordaje = NULL WHERE folio = :folio")
    suspend fun limpiarAbordaje(folio: String)
}
