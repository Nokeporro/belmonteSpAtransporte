package cl.belmontespa.myapplication.model

data class CambioPendiente(
    val estadoDestino: EstadoViaje,
    val enConflicto: Boolean = false,
    val mensajeError: String? = null,
)

fun Viaje.vigente(): Viaje {
    val cambio = cambioPendiente?.takeUnless { it.enConflicto } ?: return this
    return copy(estado = cambio.estadoDestino)
}
