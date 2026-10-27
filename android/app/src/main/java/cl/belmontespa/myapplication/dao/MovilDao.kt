package cl.belmontespa.myapplication.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cl.belmontespa.myapplication.model.MovilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovilDao {
    /** Falla con una restricción si la patente (PK) o el código de abordaje (UQ) ya existen. */
    @Insert
    suspend fun insertar(movil: MovilEntity)

    @Upsert
    suspend fun guardar(movil: MovilEntity)

    @Query("SELECT * FROM moviles WHERE patente = :patente")
    suspend fun obtener(patente: String): MovilEntity?

    @Query("SELECT * FROM moviles ORDER BY patente")
    fun observarTodos(): Flow<List<MovilEntity>>
}
