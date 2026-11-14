package cl.belmontespa.myapplication.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.belmontespa.myapplication.model.OperacionPendienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperacionPendienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(operacion: OperacionPendienteEntity)

    @Query("SELECT * FROM operaciones_pendientes WHERE cuenta = :cuenta ORDER BY creado_en")
    fun observarDeCuenta(cuenta: String): Flow<List<OperacionPendienteEntity>>

    @Query("SELECT * FROM operaciones_pendientes WHERE folio = :folio ORDER BY creado_en")
    fun observarDelViaje(folio: String): Flow<List<OperacionPendienteEntity>>

    @Query("SELECT * FROM operaciones_pendientes WHERE cuenta = :cuenta AND estado_envio = 'PENDIENTE' ORDER BY creado_en")
    suspend fun pendientesDeLaCuenta(cuenta: String): List<OperacionPendienteEntity>

    @Query("SELECT DISTINCT folio FROM operaciones_pendientes WHERE cuenta = :cuenta AND estado_envio = 'CONFLICTO'")
    suspend fun foliosEnConflicto(cuenta: String): List<String>

    @Query("SELECT COUNT(*) FROM operaciones_pendientes WHERE folio = :folio")
    suspend fun contarDelViaje(folio: String): Int

    @Query("SELECT COUNT(*) FROM operaciones_pendientes WHERE cuenta = :cuenta COLLATE NOCASE")
    suspend fun contarDeLaCuenta(cuenta: String): Int

    @Query("SELECT COUNT(*) FROM operaciones_pendientes WHERE cuenta <> :cuenta COLLATE NOCASE")
    suspend fun contarDeOtraCuenta(cuenta: String): Int

    @Query("UPDATE operaciones_pendientes SET estado_envio = 'CONFLICTO', mensaje_error = :mensaje WHERE id = :id")
    suspend fun marcarConflicto(id: String, mensaje: String?)

    @Query("DELETE FROM operaciones_pendientes WHERE id = :id")
    suspend fun borrar(id: String)

    @Query("DELETE FROM operaciones_pendientes WHERE folio = :folio")
    suspend fun borrarDelViaje(folio: String)
}
