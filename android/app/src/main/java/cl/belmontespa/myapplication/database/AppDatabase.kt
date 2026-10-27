package cl.belmontespa.myapplication.database

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.dao.HabilitacionDao
import cl.belmontespa.myapplication.dao.MovilDao
import cl.belmontespa.myapplication.dao.OperacionPendienteDao
import cl.belmontespa.myapplication.dao.PagoDao
import cl.belmontespa.myapplication.dao.PasajeroDao
import cl.belmontespa.myapplication.dao.UsuarioDao
import cl.belmontespa.myapplication.dao.ViajeDao
import cl.belmontespa.myapplication.dao.ViajePasajeroDao
import cl.belmontespa.myapplication.model.HabilitacionEntity
import cl.belmontespa.myapplication.model.MovilEntity
import cl.belmontespa.myapplication.model.OperacionPendienteEntity
import cl.belmontespa.myapplication.model.PagoEntity
import cl.belmontespa.myapplication.model.PasajeroEntity
import cl.belmontespa.myapplication.model.UsuarioEntity
import cl.belmontespa.myapplication.model.ViajeEntity
import cl.belmontespa.myapplication.model.ViajePasajeroEntity

/**
 * `belmonte_local.db`, versión 2. En la build demo es la fuente de verdad; en la remota guarda solo
 * los viajes consultados y la cola. Un cambio de esquema sube la versión con su Migration (el esquema
 * exportado queda en app/schemas/), nunca con fallbackToDestructiveMigration.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        MovilEntity::class,
        HabilitacionEntity::class,
        ViajeEntity::class,
        PasajeroEntity::class,
        ViajePasajeroEntity::class,
        PagoEntity::class,
        OperacionPendienteEntity::class,
    ],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun movilDao(): MovilDao
    abstract fun habilitacionDao(): HabilitacionDao
    abstract fun viajeDao(): ViajeDao
    abstract fun pasajeroDao(): PasajeroDao
    abstract fun viajePasajeroDao(): ViajePasajeroDao
    abstract fun pagoDao(): PagoDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao

    companion object {
        const val NOMBRE = "belmonte_local.db"

        @Volatile
        private var instancia: AppDatabase? = null

        /**
         * La base única de la app. `DatosDemo` se carga en las builds debug y demo; en release, Room guarda lo que
         * llega del backend.
         */
        fun getDatabase(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: crear(context.applicationContext).also { instancia = it }
            }

        private fun crear(context: Context): AppDatabase {
            val constructor = Room.databaseBuilder(context, AppDatabase::class.java, NOMBRE)
                .addMigrations(MIGRACION_1_2)
            if (BuildConfig.CON_SEMILLA) constructor.addCallback(SemillaDemo())
            return constructor.build()
        }

        /** Solo para pruebas: cambia la base de la app por una en memoria (o `null` para volver a la real). */
        @VisibleForTesting
        fun reemplazarParaPruebas(db: AppDatabase?) {
            instancia = db
        }

        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN password_temporal_sha256 TEXT")
                db.execSQL("ALTER TABLE usuarios ADD COLUMN password_temporal_expira INTEGER")
            }
        }
    }
}
