package cl.belmontespa.myapplication.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cl.belmontespa.myapplication.model.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insertar(usuario: UsuarioEntity)

    @Upsert
    suspend fun guardar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    suspend fun obtener(correo: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    fun observar(correo: String): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios WHERE rol = :rol ORDER BY nombre")
    fun observarPorRol(rol: String): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE rol = 'SOLICITANTE' AND estado_solicitante = 'PENDIENTE'")
    suspend fun listarSolicitantesPendientes(): List<UsuarioEntity>
}
