package cl.belmontespa.myapplication.model

import java.util.Locale

/**
 * Aplicaciones a las que el Conductor puede pasar la ruta (README, Navegar): reciben las coordenadas por Intent y
 * trazan la ruta. El orden es el de la hoja «Abrir con».
 */
enum class AppNavegacion(val nombre: String, val paquete: String) {
    GOOGLE_MAPS("Google Maps", "com.google.android.apps.maps"),
    WAZE("Waze", "com.waze");

    /** URI del `ACTION_VIEW` que traza la ruta hasta `punto`. */
    fun uri(punto: Ubicacion): String {
        val coordenadas = "${coordenada(punto.latitud)},${coordenada(punto.longitud)}"
        return when (this) {
            GOOGLE_MAPS -> "google.navigation:q=$coordenadas"
            WAZE -> "https://waze.com/ul?ll=$coordenadas&navigate=yes"
        }
    }
}

// Punto decimal y sin notación científica, sea cual sea el idioma del teléfono.
private fun coordenada(valor: Double): String = String.format(Locale.ROOT, "%.6f", valor)

/**
 * Adónde lleva «Navegar» (README, Detalle del Conductor): al origen mientras el viaje está `ASIGNADO` y al destino
 * cuando está `EN_CURSO`. En cualquier otro estado no hay ruta que trazar.
 */
fun Viaje.puntoDeNavegacion(): Ubicacion? = when (estado) {
    EstadoViaje.ASIGNADO -> origen
    EstadoViaje.EN_CURSO -> destino
    else -> null
}
