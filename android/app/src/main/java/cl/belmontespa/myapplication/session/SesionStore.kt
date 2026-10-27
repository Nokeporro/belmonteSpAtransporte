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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Sesión guardada en Preferences DataStore (`sesion.preferences_pb`): `token`, `correo`, `rol`,
 * `expiraEn` y `ultimaCuenta`. Nunca guarda contraseñas. La sesión de una contraseña temporal vive
 * solo en memoria, así que se pierde al cerrar la app.
 */
class SesionStore(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) {
    // La sesión de una contraseña temporal: vive solo en memoria y manda sobre la guardada.
    private val temporal = MutableStateFlow<Sesion?>(null)

    /**
     * El estado de la sesión, en dos pasos:
     * 1. `combine` junta lo guardado en DataStore con la sesión temporal (si hay una, esa manda).
     * 2. `flatMapLatest` emite ese estado y, cuando la sesión vence, `SinSesion`. Si la sesión cambia antes, descarta
     *    la espera anterior y arranca la de la nueva.
     * Una sesión vencida cuenta como sin sesión: la app abre el Login.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: Flow<EstadoSesion> =
        combine(dataStore.data, temporal) { preferencias, sesionTemporal ->
            if (sesionTemporal != null) EstadoSesion.Temporal(sesionTemporal)
            else leer(preferencias)?.let { EstadoSesion.Autenticada(it) }
        }.flatMapLatest { vigente -> hastaQueVenza(vigente) }.distinctUntilChanged()

    // Emite el estado mientras la sesión no haya vencido y, al vencer, SinSesion. La espera es local: no consulta al backend.
    private fun hastaQueVenza(vigente: EstadoSesion?): Flow<EstadoSesion> = flow {
        val sesion = when (vigente) {
            is EstadoSesion.Temporal -> vigente.sesion
            is EstadoSesion.Autenticada -> vigente.sesion
            else -> null
        }
        val restante = if (sesion == null) 0L else sesion.expiraEn - clock.millis()
        if (vigente != null && restante > 0) {
            emit(vigente)
            delay(restante)
        }
        emit(EstadoSesion.SinSesion)
    }

    /** Correo de la última cuenta que entró, para detectar un cambio de cuenta. */
    val ultimaCuenta: Flow<String?> =
        dataStore.data.map { it[ULTIMA_CUENTA] }.distinctUntilChanged()

    suspend fun guardar(sesion: Sesion) {
        // Primero confirma la escritura; luego retira la sesión temporal.
        dataStore.edit {
            it[TOKEN] = sesion.token
            it[CORREO] = sesion.correo
            it[ROL] = sesion.rol.name
            it[EXPIRA_EN] = sesion.expiraEn
            it[ULTIMA_CUENTA] = sesion.correo
        }
        temporal.value = null
    }

    /** Elimina la sesión persistida anterior antes de aceptar una temporal. */
    suspend fun guardarTemporal(sesion: Sesion) {
        borrarGuardada()
        // No se escribe el token ni la cuenta temporal en DataStore.
        temporal.value = sesion
    }

    /** Cierre de sesión local: borra la sesión y conserva `ultimaCuenta`. */
    suspend fun borrar() {
        borrarGuardada()
        // ultimaCuenta permanece para las reglas de cambio de cuenta.
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

        /** Archivo `datastore/sesion.preferences_pb`, excluido de los respaldos. */
        fun getInstance(context: Context): SesionStore =
            instancia ?: synchronized(this) {
                instancia ?: SesionStore(
                    PreferenceDataStoreFactory.create { context.applicationContext.preferencesDataStoreFile("sesion") },
                    Reloj.santiago,
                ).also { instancia = it }
            }

        /** Solo para pruebas: cambia la sesión de la app por una en memoria (o `null` para volver a la real). */
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
