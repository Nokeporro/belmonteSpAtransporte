package cl.belmontespa.myapplication.ui.navigation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import cl.belmontespa.myapplication.data.local.dao.*
import cl.belmontespa.myapplication.data.local.database.*
import cl.belmontespa.myapplication.di.BaseDatosModule
import cl.belmontespa.myapplication.di.SesionModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Room y DataStore de las pruebas no escriben sobre las cuentas del usuario.
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [BaseDatosModule::class, SesionModule::class])
object DatosNavegacionModule {
    @Provides @Singleton fun base(@ApplicationContext contexto: Context, reloj: Clock): BelmonteDatabase =
        Room.inMemoryDatabaseBuilder(contexto, BelmonteDatabase::class.java).addCallback(SemillaDemo(reloj)).build()
    @Provides @Singleton fun preferencias(): DataStore<Preferences> = PreferenciasNavegacion()
    @Provides fun usuarios(db: BelmonteDatabase): UsuarioDao = db.usuarioDao()
    @Provides fun moviles(db: BelmonteDatabase): MovilDao = db.movilDao()
    @Provides fun habilitaciones(db: BelmonteDatabase): HabilitacionDao = db.habilitacionDao()
    @Provides fun viajes(db: BelmonteDatabase): ViajeDao = db.viajeDao()
    @Provides fun pasajeros(db: BelmonteDatabase): PasajeroDao = db.pasajeroDao()
    @Provides fun detalles(db: BelmonteDatabase): ViajePasajeroDao = db.viajePasajeroDao()
    @Provides fun pagos(db: BelmonteDatabase): PagoDao = db.pagoDao()
    @Provides fun cola(db: BelmonteDatabase): OperacionPendienteDao = db.operacionPendienteDao()
}
private class PreferenciasNavegacion : DataStore<Preferences> {
    private val contenido = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override val data: Flow<Preferences> = contenido
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(contenido.value).also { contenido.value = it }
    }
}