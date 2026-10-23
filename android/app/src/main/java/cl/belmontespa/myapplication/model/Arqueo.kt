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

/** El arqueo solo existe con un conductor asignado o después (README, RF29): ni `PENDIENTE` ni `CANCELADO` lo tienen. */
private val ESTADOS_CON_ARQUEO = setOf(EstadoViaje.ASIGNADO, EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO)

/** Sin pasajeros o fuera de esos estados no hay nada que contar. Los que faltan van en el orden de `pasajeros` (por nombre). */
fun Viaje.arqueo(): Arqueo? {
    if (estado !in ESTADOS_CON_ARQUEO || cantidadPasajeros == 0) return null
    return Arqueo(
        abordaron = pasajeros.count { it.fhAbordaje != null },
        total = cantidadPasajeros,
        faltan = pasajeros.filter { it.fhAbordaje == null }.map { it.nombre },
    )
}
