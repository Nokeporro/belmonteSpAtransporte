package cl.belmontespa.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.belmontespa.myapplication.data.local.entity.HabilitacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabilitacionDao {
    /** Una habilitación existente se ignora sin duplicar: devuelve -1 en ese caso. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(habilitacion: HabilitacionEntity): Long

    @Query("SELECT * FROM habilitaciones WHERE patente = :patente")
    fun observarDelMovil(patente: String): Flow<List<HabilitacionEntity>>

    @Query("SELECT * FROM habilitaciones WHERE correo_conductor = :correo ORDER BY patente")
    fun observarDelConductor(correo: String): Flow<List<HabilitacionEntity>>
}
