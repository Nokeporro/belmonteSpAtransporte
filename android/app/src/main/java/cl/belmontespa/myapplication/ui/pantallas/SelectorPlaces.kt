package cl.belmontespa.myapplication.ui.pantallas

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.ui.components.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.PlaceAutocomplete
import java.math.RoundingMode
import java.util.Locale

/** La búsqueda y sus coordenadas vienen de Places, también en la build demo. */
@Composable
internal fun ContextoPantalla.SelectorPlaces(campo: String, @StringRes etiqueta: Int) {
    val contexto = LocalContext.current
    var buscando by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intent = result.data
        if (result.resultCode == Activity.RESULT_OK && intent != null) {
            val prediccion = PlaceAutocomplete.getPredictionFromIntent(intent)
            if (prediccion != null) {
                buscando = true
                val request = FetchPlaceRequest.builder(prediccion.placeId, listOf(Place.Field.DISPLAY_NAME,
                    Place.Field.LOCATION, Place.Field.ADDRESS_COMPONENTS, Place.Field.FORMATTED_ADDRESS, Place.Field.TYPES))
                    .setSessionToken(PlaceAutocomplete.getSessionTokenFromIntent(intent)).build()
                Places.createClient(contexto).fetchPlace(request).addOnSuccessListener { response ->
                    buscando = false
                    val place = response.place
                    val punto = place.location
                    val componentes = place.addressComponents?.asList().orEmpty()
                    fun componente(tipo: String) = componentes.firstOrNull { tipo in it.types }?.name.orEmpty()
                    val calle = listOf(componente("route"), componente("street_number")).filter(String::isNotBlank).joinToString(" ")
                    val establecimiento = place.placeTypes.orEmpty().any { it == "establishment" || it == "point_of_interest" }
                    val direccion = (if (establecimiento) place.displayName else calle).orEmpty()
                        .ifBlank { place.formattedAddress.orEmpty().substringBefore(',') }.take(200)
                    if (punto == null || direccion.isBlank()) vm.aviso(R.string.error_places)
                    else {
                        fun redondear(n: Double) = n.toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()
                        vm.seleccionarLugar(campo, Ubicacion(direccion,
                            componente("administrative_area_level_3").ifBlank { componente("locality") },
                            componente("administrative_area_level_1"), redondear(punto.latitude), redondear(punto.longitude)))
                    }
                }.addOnFailureListener { buscando = false; vm.aviso(R.string.error_places) }
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) vm.aviso(R.string.error_places)
    }
    val seleccionado = estado.ubicaciones[campo]
    Dato(etiqueta, seleccionado?.direccion)
    BotonAncho(stringResource(etiqueta), {
        if (BuildConfig.PLACES_API_KEY.isBlank()) vm.aviso(R.string.error_places_configuracion)
        else {
            if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(contexto.applicationContext,
                BuildConfig.PLACES_API_KEY, Locale.forLanguageTag("es-CL"))
            launcher.launch(PlaceAutocomplete.IntentBuilder().setCountries(listOf("CL")).setRegionCode("CL").build(contexto))
        }
    }, habilitado = !buscando && !estado.enviando, tipo = TipoBoton.SECUNDARIO)
    if (buscando) Text(stringResource(R.string.places_buscando))
    if (seleccionado != null) {
        // Corrige solo el catálogo: nunca modifica dirección ni coordenadas elegidas en Places.
        if (seleccionado.comuna.isBlank()) Aviso(stringResource(R.string.places_comuna_faltante), TonoAviso.AVISO)
        val regiones = estado.catalogos?.regiones.orEmpty()
        Selector("$campo.region", R.string.campo_region, regiones.map { Opcion(it.nombre, it.nombre) }, alElegir = { vm.seleccionarRegion(campo) })
        Selector("$campo.comuna", R.string.campo_comuna,
            regiones.find { it.nombre == vm.valor("$campo.region") }?.comunas.orEmpty().map { Opcion(it, it) },
            alElegir = { vm.corregirComuna(campo, vm.valor("$campo.comuna")) })
    }
}
