package cl.belmontespa.myapplication.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.belmontespa.myapplication.data.local.dao.HabilitacionDao
import cl.belmontespa.myapplication.data.local.dao.MovilDao
import cl.belmontespa.myapplication.data.local.dao.OperacionPendienteDao
import cl.belmontespa.myapplication.data.local.dao.PagoDao
import cl.belmontespa.myapplication.data.local.dao.PasajeroDao
import cl.belmontespa.myapplication.data.local.dao.UsuarioDao
import cl.belmontespa.myapplication.data.local.dao.ViajeDao
import cl.belmontespa.myapplication.data.local.dao.ViajePasajeroDao
import cl.belmontespa.myapplication.data.local.entity.HabilitacionEntity
import cl.belmontespa.myapplication.data.local.entity.MovilEntity
import cl.belmontespa.myapplication.data.local.entity.OperacionPendienteEntity
import cl.belmontespa.myapplication.data.local.entity.PagoEntity
import cl.belmontespa.myapplication.data.local.entity.PasajeroEntity
import cl.belmontespa.myapplication.data.local.entity.UsuarioEntity
import cl.belmontespa.myapplication.data.local.entity.ViajeEntity
import cl.belmontespa.myapplication.data.local.entity.ViajePasajeroEntity

/**
 * `belmonte_local.db`, versión 1. En la build demo es la fuente de verdad; en la remota guarda solo
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
    version = 1,
)
abstract class BelmonteDatabase : RoomDatabase() {
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
    }
}
