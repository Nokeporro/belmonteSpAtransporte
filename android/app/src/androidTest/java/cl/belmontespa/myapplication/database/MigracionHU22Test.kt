package cl.belmontespa.myapplication.database

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigracionHU22Test {
    @Test fun versionUnoMigraSinBorrarLaCuentaNiSuContrasena() = runBlocking {
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        val contexto = instrumentacion.targetContext
        val nombre = "migracion-hu22-prueba.db"
        contexto.deleteDatabase(nombre)
        val esquema = JSONObject(instrumentacion.context.assets.open(
            "cl.belmontespa.myapplication.database.AppDatabase/1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        val anterior = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(contexto)
            .name(nombre).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entidades = esquema.getJSONArray("entities")
                    for (i in 0 until entidades.length()) {
                        val entidad = entidades.getJSONObject(i)
                        db.execSQL(entidad.getString("createSql").replace("\${TABLE_NAME}", entidad.getString("tableName")))
                        val indices = entidad.optJSONArray("indices") ?: JSONArray()
                        for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql")
                            .replace("\${TABLE_NAME}", entidad.getString("tableName")))
                    }
                    val consultas = esquema.getJSONArray("setupQueries")
                    for (i in 0 until consultas.length()) db.execSQL(consultas.getString(i))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Solo crea v1")
            }).build())
        try {
            anterior.writableDatabase.execSQL("INSERT INTO usuarios (correo,nombre,rol,activo,fecha_registro,password_sha256) VALUES ('migracion@test.cl','Cuenta conservada','ADMINISTRADOR',1,'2026-10-24T12:00:00','hash-anterior')")
            anterior.close()
            val nueva = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre)
                .addMigrations(AppDatabase.MIGRACION_1_2).build()
            try {
                val usuario = nueva.usuarioDao().obtener("migracion@test.cl")!!
                assertEquals("Cuenta conservada", usuario.nombre); assertEquals("hash-anterior", usuario.passwordSha256)
                assertNull(usuario.passwordTemporalSha256); assertNull(usuario.passwordTemporalExpira)
                assertEquals(2, nueva.openHelper.readableDatabase.version)
            } finally { nueva.close() }
        } finally { anterior.close(); contexto.deleteDatabase(nombre) }
    }
}
