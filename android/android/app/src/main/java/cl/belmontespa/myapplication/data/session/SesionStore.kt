package cl.belmontespa.myapplication.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Sesión guardada en Preferences DataStore (`sesion.preferences_pb`): `token`, `correo`, `rol`,
 * `expiraEn` y `ultimaCuenta`. Nunca guarda contraseñas. La sesión de una contraseña temporal vive
 * solo en memoria, así que se pierde al cerrar la app.
 */
@Singleton
class SesionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) {
    private val temporal = MutableStateFlow<Sesion?>(null)

    /** Una sesión vencida cuenta como sin sesión: la app abre el Login. */
    val estado: Flow<EstadoSesion> = combine(dataStore.data, temporal) { preferencias, temporal ->
        val guardada = leer(preferencias)
        when {
            temporal != null -> EstadoSesion.Temporal(temporal)
            guardada != null && guardada.expiraEn > clock.millis() -> EstadoSesion.Autenticada(guardada)
            else -> EstadoSesion.SinSesion
        }
    }

    /** Correo de la última cuenta que entró, para saber si cambió la cuenta en este teléfono. */
    val ultimaCuenta: Flow<String?> = dataStore.data.map { it[ULTIMA_CUENTA] }

    suspend fun guardar(sesion: Sesion) {
        temporal.value = null
        dataStore.edit {
            it[TOKEN] = sesion.token
            it[CORREO] = sesion.correo
            it[ROL] = sesion.rol.name
            it[EXPIRA_EN] = sesion.expiraEn
            it[ULTIMA_CUENTA] = sesion.correo
        }
    }

    fun guardarTemporal(sesion: Sesion) {
        temporal.value = sesion
    }

    /** Cierre de sesión local: borra la sesión y conserva `ultimaCuenta`. */
    suspend fun borrar() {
        temporal.value = null
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

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val CORREO = stringPreferencesKey("correo")
        val ROL = stringPreferencesKey("rol")
        val EXPIRA_EN = longPreferencesKey("expiraEn")
        val ULTIMA_CUENTA = stringPreferencesKey("ultimaCuenta")
    }
}
