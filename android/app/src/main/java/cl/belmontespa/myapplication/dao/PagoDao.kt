package cl.belmontespa.myapplication.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cl.belmontespa.myapplication.model.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoDao {
    /** Falla con una restricción si el viaje ya tiene pago (UQ `folio`). */
    @Insert
    suspend fun insertar(pago: PagoEntity)

    @Upsert
    suspend fun guardar(pago: PagoEntity)

    @Query("SELECT * FROM pagos WHERE hash = :hash")
    suspend fun obtener(hash: String): PagoEntity?

    @Query("SELECT * FROM pagos WHERE folio = :folio")
    suspend fun obtenerDelViaje(folio: String): PagoEntity?

    /** Por folio descendente, según su número (`TR-9999` antes que `TR-10000`). */
    @Query("SELECT * FROM pagos ORDER BY CAST(substr(folio, 4) AS INTEGER) DESC")
    fun observarTodos(): Flow<List<PagoEntity>>
}
