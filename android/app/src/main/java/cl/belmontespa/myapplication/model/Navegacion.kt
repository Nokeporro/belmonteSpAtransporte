package cl.belmontespa.myapplication.model

import java.util.Locale

enum class AppNavegacion(val nombre: String, val paquete: String) {
    GOOGLE_MAPS("Google Maps", "com.google.android.apps.maps"),
    WAZE("Waze", "com.waze");

    fun uri(punto: Ubicacion): String {
        val coordenadas = "${coordenada(punto.latitud)},${coordenada(punto.longitud)}"
        return when (this) {
            GOOGLE_MAPS -> "google.navigation:q=$coordenadas"
            WAZE -> "https://waze.com/ul?ll=$coordenadas&navigate=yes"
        }
    }
}

private fun coordenada(valor: Double): String = String.format(Locale.ROOT, "%.6f", valor)

fun Viaje.puntoDeNavegacion(): Ubicacion? = when (estado) {
    EstadoViaje.ASIGNADO -> origen
    EstadoViaje.EN_CURSO -> destino
    else -> null
}
