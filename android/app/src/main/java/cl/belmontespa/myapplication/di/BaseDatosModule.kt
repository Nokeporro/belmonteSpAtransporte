package cl.belmontespa.myapplication.di

import android.content.Context
import androidx.room.Room
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.data.local.dao.HabilitacionDao
import cl.belmontespa.myapplication.data.local.dao.MovilDao
import cl.belmontespa.myapplication.data.local.dao.OperacionPendienteDao
import cl.belmontespa.myapplication.data.local.dao.PagoDao
import cl.belmontespa.myapplication.data.local.dao.PasajeroDao
import cl.belmontespa.myapplication.data.local.dao.UsuarioDao
import cl.belmontespa.myapplication.data.local.dao.ViajeDao
import cl.belmontespa.myapplication.data.local.dao.ViajePasajeroDao
import cl.belmontespa.myapplication.data.local.database.BelmonteDatabase
import cl.belmontespa.myapplication.data.local.database.SemillaDemo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseDatosModule {
    @Provides
    @Singleton
    fun baseDatos(@ApplicationContext context: Context, semilla: SemillaDemo): BelmonteDatabase {
        val constructor = Room.databaseBuilder(context, BelmonteDatabase::class.java, BelmonteDatabase.NOMBRE)
            .addMigrations(BelmonteDatabase.MIGRACION_1_2)
        // DatosDemo solo va en la build demo; en la remota, Room guarda lo que llega del backend.
        if (BuildConfig.MODO_LOCAL) constructor.addCallback(semilla)
        return constructor.build()
    }

    @Provides
    fun usuarioDao(db: BelmonteDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun movilDao(db: BelmonteDatabase): MovilDao = db.movilDao()

    @Provides
    fun habilitacionDao(db: BelmonteDatabase): HabilitacionDao = db.habilitacionDao()

    @Provides
    fun viajeDao(db: BelmonteDatabase): ViajeDao = db.viajeDao()

    @Provides
    fun pasajeroDao(db: BelmonteDatabase): PasajeroDao = db.pasajeroDao()

    @Provides
    fun viajePasajeroDao(db: BelmonteDatabase): ViajePasajeroDao = db.viajePasajeroDao()

    @Provides
    fun pagoDao(db: BelmonteDatabase): PagoDao = db.pagoDao()

    @Provides
    fun operacionPendienteDao(db: BelmonteDatabase): OperacionPendienteDao = db.operacionPendienteDao()
}
