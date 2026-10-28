package cl.belmontespa.myapplication.session

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SesionStore(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) {
    private val temporal = MutableStateFlow<Sesion?>(null)

    val estado: Flow<EstadoSesion> =
        combine(dataStore.data, temporal) { preferencias, sesionTemporal ->
            val sesion = sesionTemporal ?: leer(preferencias)
            when {
                sesion == null || sesion.expiraEn <= clock.millis() -> EstadoSesion.SinSesion
                sesionTemporal != null -> EstadoSesion.Temporal(sesion)
                else -> EstadoSesion.Autenticada(sesion)
            }
        }.distinctUntilChanged()

    val ultimaCuenta: Flow<String?> =
        dataStore.data.map { it[ULTIMA_CUENTA] }.distinctUntilChanged()

    suspend fun guardar(sesion: Sesion) {
        dataStore.edit {
            it[TOKEN] = sesion.token
            it[CORREO] = sesion.correo
            it[ROL] = sesion.rol.name
            it[EXPIRA_EN] = sesion.expiraEn
            it[ULTIMA_CUENTA] = sesion.correo
        }
        temporal.value = null
    }

    suspend fun guardarTemporal(sesion: Sesion) {
        borrarGuardada()
        temporal.value = sesion
    }

    suspend fun borrar() {
        borrarGuardada()
        temporal.value = null
    }

    private suspend fun borrarGuardada() {
        dataStore.edit {
            it.remove(TOKEN)
            it.remove(CORREO)
            it.remove(ROL)
            it.remove(EXPIRA_EN)
        }
    }

    private fun leer(preferencias: Preferences): Sesion? {
        val token = preferencias[TOKEN] ?: return null
        val correo = preferencias[CORREO] ?: return null
        val rol = preferencias[ROL]?.let { codigo -> Rol.entries.find { it.name == codigo } } ?: return null
        val expiraEn = preferencias[EXPIRA_EN] ?: return null
        return Sesion(token, correo, rol, expiraEn)
    }

    companion object {
        @Volatile
        private var instancia: SesionStore? = null

        fun getInstance(context: Context): SesionStore =
            instancia ?: synchronized(this) {
                instancia ?: SesionStore(
                    PreferenceDataStoreFactory.create { context.applicationContext.preferencesDataStoreFile("sesion") },
                    Reloj.santiago,
                ).also { instancia = it }
            }

        @VisibleForTesting
        fun reemplazarParaPruebas(store: SesionStore?) {
            instancia = store
        }

        private val TOKEN = stringPreferencesKey("token")
        val CORREO = stringPreferencesKey("correo")
        val ROL = stringPreferencesKey("rol")
        val EXPIRA_EN = longPreferencesKey("expiraEn")
        val ULTIMA_CUENTA = stringPreferencesKey("ultimaCuenta")
    }
}
