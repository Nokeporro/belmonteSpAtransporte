package cl.belmontespa.myapplication.lugares

import cl.belmontespa.myapplication.model.Ubicacion

private const val LARGO_DIRECCION = 200

data class ComponenteDeLugar(val tipos: List<String>, val nombre: String)

data class LugarCrudo(
    val nombre: String?,
    val tipos: List<String>,
    val direccionCompleta: String?,
    val componentes: List<ComponenteDeLugar>,
    val latitud: Double,
    val longitud: Double,
)

private fun LugarCrudo.componente(tipo: String): String? =
    componentes.firstOrNull { tipo in it.tipos }?.nombre?.trim()?.takeIf { it.isNotEmpty() }

private fun LugarCrudo.direccion(): String? {
    val nombre = nombre?.trim()?.takeIf { it.isNotEmpty() }
    if (nombre != null && ("establishment" in tipos || "point_of_interest" in tipos)) return nombre
    val calle = componente("route")
    if (calle != null) return listOfNotNull(calle, componente("street_number")).joinToString(" ")
    return nombre ?: direccionCompleta?.trim()?.takeIf { it.isNotEmpty() }
}

fun LugarCrudo.aUbicacion(): Ubicacion? {
    val direccion = direccion()?.take(LARGO_DIRECCION) ?: return null
    return Ubicacion(
        direccion = direccion,
        comuna = componente("administrative_area_level_3") ?: componente("locality") ?: "",
        region = componente("administrative_area_level_1") ?: "",
        latitud = latitud,
        longitud = longitud,
    )
}
