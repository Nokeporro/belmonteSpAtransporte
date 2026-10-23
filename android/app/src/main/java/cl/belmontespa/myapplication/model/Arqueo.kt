package cl.belmontespa.myapplication.model

/**
 * Arqueo de abordaje (README, Abordaje con código QR): «Abordaron X de N» y quiénes faltan. Lo calcula la
 * app desde los `pasajeros` del viaje; `total` es `cantidadPasajeros`.
 */
data class Arqueo(
    val abordaron: Int,
    val total: Int,
    val faltan: List<String>,
) {
    val completo: Boolean get() = abordaron == total
}

/** Sin pasajeros no hay nada que contar. Los que faltan van en el orden de `pasajeros` (por nombre). */
fun Viaje.arqueo(): Arqueo? {
    if (cantidadPasajeros == 0) return null
    return Arqueo(
        abordaron = pasajeros.count { it.fhAbordaje != null },
        total = cantidadPasajeros,
        faltan = pasajeros.filter { it.fhAbordaje == null }.map { it.nombre },
    )
}
