package com.example.myapplication.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert
    suspend fun  usuarioAlmacenar(usuario: Usuario)

    @Update
    suspend fun usuarioActualizar(usuario: Usuario)

    @Delete
    suspend fun usuarioEliminar(usuario: Usuario)

    @Query("SELECT * FROM usuario ORDER BY id DESC")
    fun usuarioObtener(): Flow<List<Usuario>>

}