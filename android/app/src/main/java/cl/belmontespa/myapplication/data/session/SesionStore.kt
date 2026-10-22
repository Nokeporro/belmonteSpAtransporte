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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    // Evita que dos operaciones de sesión escriban al mismo tiempo.
    private val cambios = Mutex()

    /** Una sesión vencida cuenta como sin sesión: la app abre el Login. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: Flow<EstadoSesion> =
        combine(dataStore.data, temporal) { preferencias, sesionTemporal ->
            sesionTemporal to leer(preferencias)
        }.flatMapLatest { (sesionTemporal, guardada) ->
            // Una nueva sesión cancela la espera de vencimiento de la anterior.
            flow {
                val sesion = sesionTemporal ?: guardada

                if (sesion != null) {
                    var restante = sesion.expiraEn - clock.millis()

                    if (restante > 0) {
                        emit(
                            if (sesionTemporal != null) {
                                EstadoSesion.Temporal(sesion)
                            } else {
                                EstadoSesion.Autenticada(sesion)
                            }
                        )

                        while (restante > 0) {
                            // Revisa también cambios en el reloj del dispositivo.
                            // Esta espera es local: no consulta al backend.
                            delay(minOf(restante, 30_000L))
                            restante = sesion.expiraEn - clock.millis()
                        }
                    }
                }

                emit(EstadoSesion.SinSesion)
            }
        }.distinctUntilChanged()

    /** Correo de la última cuenta que entró, para detectar un cambio de cuenta. */
    val ultimaCuenta: Flow<String?> =
        dataStore.data.map { it[ULTIMA_CUENTA] }.distinctUntilChanged()

    suspend fun guardar(sesion: Sesion) {
        cambios.withLock {
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
    }

    /** Elimina la sesión persistida anterior antes de aceptar una temporal. */
    suspend fun guardarTemporal(sesion: Sesion) {
        cambios.withLock {
            dataStore.edit {
                it.remove(TOKEN)
                it.remove(CORREO)
                it.remove(ROL)
                it.remove(EXPIRA_EN)
            }

            // No se escribe el token ni la cuenta temporal en DataStore.
            temporal.value = sesion
        }
    }

    /** Cierre de sesión local: borra la sesión y conserva `ultimaCuenta`. */
    suspend fun borrar() {
        cambios.withLock {
            dataStore.edit {
                it.remove(TOKEN)
                it.remove(CORREO)
                it.remove(ROL)
                it.remove(EXPIRA_EN)
            }

            // ultimaCuenta permanece para las reglas de cambio de cuenta.
            temporal.value = null
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
