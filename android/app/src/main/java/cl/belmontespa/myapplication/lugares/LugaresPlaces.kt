package cl.belmontespa.myapplication.lugares

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.kotlin.awaitFetchPlace
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CancellationException

class LugaresPlaces private constructor(private val aplicacion: Application) : ServicioDeLugares {

    @Volatile
    private var sesion: AutocompleteSessionToken? = null

    override val disponible: Boolean get() = habilitadoPorDefecto

    override fun intentDeBusqueda(contexto: Context): Intent {
        iniciar()
        return PlaceAutocomplete.createIntent(contexto) { setCountries(listOf(PAIS)) }
    }

    override fun resultadoDeBusqueda(codigo: Int, datos: Intent?): ResultadoDeBusqueda = when {
        codigo == PlaceAutocompleteActivity.RESULT_OK && datos != null -> {
            val prediccion = PlaceAutocomplete.getPredictionFromIntent(datos)
            if (prediccion == null) {
                ResultadoDeBusqueda.Fallido
            } else {
                sesion = PlaceAutocomplete.getSessionTokenFromIntent(datos)
                ResultadoDeBusqueda.Elegido(prediccion.placeId)
            }
        }
        codigo == PlaceAutocompleteActivity.RESULT_ERROR -> ResultadoDeBusqueda.Fallido
        else -> ResultadoDeBusqueda.Cancelado
    }

    override suspend fun obtener(placeId: String): Resultado<LugarCrudo> {
        val token = sesion
        sesion = null
        return try {
            iniciar()
            val respuesta = Places.createClient(aplicacion).awaitFetchPlace(placeId, CAMPOS) {
                token?.let { setSessionToken(it) }
            }
            respuesta.place.aLugarCrudo()?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorApp.Inesperado)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Resultado.Fallo(if (e.statusCode == CommonStatusCodes.NETWORK_ERROR) ErrorApp.SinConexion else ErrorApp.Inesperado)
        } catch (_: IOException) {
            Resultado.Fallo(ErrorApp.SinConexion)
        } catch (_: Exception) {
            Resultado.Fallo(ErrorApp.Inesperado)
        }
    }

    private fun iniciar() {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(aplicacion, BuildConfig.PLACES_API_KEY, Locale.forLanguageTag("es-CL"))
        }
    }

    private fun Place.aLugarCrudo(): LugarCrudo? {
        val punto = location ?: return null
        return LugarCrudo(
            nombre = displayName,
            tipos = placeTypes.orEmpty(),
            direccionCompleta = formattedAddress,
            componentes = addressComponents?.asList().orEmpty().map { ComponenteDeLugar(it.types.orEmpty(), it.name) },
            latitud = punto.latitude,
            longitud = punto.longitude,
        )
    }

    companion object {
        private const val PAIS = "CL"
        private val CAMPOS = listOf(
            Place.Field.DISPLAY_NAME,
            Place.Field.ADDRESS_COMPONENTS,
            Place.Field.LOCATION,
            Place.Field.TYPES,
            Place.Field.FORMATTED_ADDRESS,
        )

        @VisibleForTesting
        var habilitadoPorDefecto: Boolean = BuildConfig.PLACES_CONFIGURADO

        @Volatile
        private var instancia: LugaresPlaces? = null

        fun de(application: Application): LugaresPlaces =
            instancia ?: synchronized(this) { instancia ?: LugaresPlaces(application).also { instancia = it } }
    }
}
