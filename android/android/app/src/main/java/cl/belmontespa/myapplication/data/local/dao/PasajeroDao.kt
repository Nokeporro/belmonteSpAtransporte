package cl.belmontespa.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.belmontespa.myapplication.data.local.entity.PasajeroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PasajeroDao {
    /** Un documento ya registrado no se duplica ni cambia su nombre: devuelve -1 en ese caso. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(pasajero: PasajeroEntity): Long

    @Query("SELECT * FROM pasajeros WHERE dni_hash = :dniHash")
    suspend fun obtener(dniHash: String): PasajeroEntity?

    @Query("SELECT * FROM pasajeros ORDER BY nombre")
    fun observarTodos(): Flow<List<PasajeroEntity>>
}
