package cl.belmontespa.myapplication.model

data class Arqueo(
    val abordaron: Int,
    val total: Int,
    val faltan: List<String>,
) {
    val completo: Boolean get() = abordaron == total
}

private val ESTADOS_CON_ARQUEO = setOf(EstadoViaje.ASIGNADO, EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO)

fun Viaje.arqueo(): Arqueo? {
    if (estado !in ESTADOS_CON_ARQUEO || cantidadPasajeros == 0) return null
    return Arqueo(
        abordaron = pasajeros.count { it.fhAbordaje != null },
        total = cantidadPasajeros,
        faltan = pasajeros.filter { it.fhAbordaje == null }.map { it.nombre },
    )
}
