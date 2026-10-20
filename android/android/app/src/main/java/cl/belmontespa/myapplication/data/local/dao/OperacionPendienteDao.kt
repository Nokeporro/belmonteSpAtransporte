package cl.belmontespa.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.belmontespa.myapplication.data.local.entity.OperacionPendienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperacionPendienteDao {
    @Insert
    suspend fun insertar(operacion: OperacionPendienteEntity)

    /** Cola de una cuenta en orden de creación, como la envía el Worker. */
    @Query("SELECT * FROM operaciones_pendientes WHERE cuenta = :cuenta ORDER BY creado_en")
    fun observarDeCuenta(cuenta: String): Flow<List<OperacionPendienteEntity>>

    @Query("UPDATE operaciones_pendientes SET estado_envio = 'CONFLICTO', mensaje_error = :mensaje WHERE id = :id")
    suspend fun marcarConflicto(id: String, mensaje: String?)

    @Query("DELETE FROM operaciones_pendientes WHERE id = :id")
    suspend fun borrar(id: String)

    @Query("DELETE FROM operaciones_pendientes WHERE folio = :folio")
    suspend fun borrarDelViaje(folio: String)
}
