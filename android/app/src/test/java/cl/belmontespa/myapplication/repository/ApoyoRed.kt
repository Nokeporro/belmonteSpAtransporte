package cl.belmontespa.myapplication.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.session.SesionStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PreferenciasEnMemoria : DataStore<Preferences> {
    private val contenido = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override val data: Flow<Preferences> = contenido
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(contenido.value).also { contenido.value = it }
    }
}

fun sesionesEnMemoria() = SesionStore(PreferenciasEnMemoria(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

fun sesionDePrueba(token: String = "jwt-de-prueba") = Sesion(token, "ana@belmontespa.cl", Rol.ADMINISTRADOR, expiraEn = 3_600_000)
