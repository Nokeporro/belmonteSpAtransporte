package cl.belmontespa.myapplication

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.belmontespa.myapplication.database.AppDatabase
import cl.belmontespa.myapplication.database.SemillaDemo
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.session.SesionStore
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.rules.ExternalResource

class EntornoPruebas(private val reloj: Clock = Reloj.santiago) : ExternalResource() {
    lateinit var db: AppDatabase
        private set
    lateinit var sesiones: SesionStore
        private set
    lateinit var local: SoporteLocal
        private set

    override fun before() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).addCallback(SemillaDemo(reloj)).build()
        sesiones = SesionStore(PreferenciasEnMemoria(), reloj)
        AppDatabase.reemplazarParaPruebas(db)
        SesionStore.reemplazarParaPruebas(sesiones)
        local = SoporteLocal(db, sesiones, reloj)
    }

    override fun after() {
        AppDatabase.reemplazarParaPruebas(null)
        SesionStore.reemplazarParaPruebas(null)
        db.close()
    }
}

private class PreferenciasEnMemoria : DataStore<Preferences> {
    private val contenido = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override val data: Flow<Preferences> = contenido
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(contenido.value).also { contenido.value = it }
    }
}
