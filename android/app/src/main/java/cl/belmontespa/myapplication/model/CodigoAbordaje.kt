package cl.belmontespa.myapplication.model

/**
 * Lo que identifica al vehículo en el abordaje: la patente que trae el QR de la puerta o la que se escribe a mano.
 * Ambos caminos terminan aquí, siempre normalizada.
 */
data class CodigoAbordaje(
    val patente: String,
) {
    /** Forma en que viaja hacia «Confirmar abordaje», como argumento de la ruta. */
    fun paraRuta(): String = patente

    companion object {
        private const val PREFIJO_QR = "BELMONTE-ABORDAJE"
        private val PATENTE = Regex("[A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2}")
        private val ESPACIOS_Y_GUIONES = Regex("[\\s-]")

        /**
         * `BELMONTE-ABORDAJE:{patente}`; en mayúsculas debe cumplir
         * `^BELMONTE-ABORDAJE:([A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2})$`. Otro contenido, `null`.
         */
        fun desdeQr(contenido: String): CodigoAbordaje? {
            val partes = contenido.uppercase().split(':')
            if (partes.size != 2 || partes[0] != PREFIJO_QR) return null
            return crear(partes[1])
        }

        /** Patente escrita a mano: sin espacios ni guion, en mayúsculas. */
        fun manual(patente: String): CodigoAbordaje? = crear(patente.replace(ESPACIOS_Y_GUIONES, "").uppercase())

        /** Inversa de [paraRuta]; `null` si el argumento no es una patente válida. */
        fun desdeRuta(argumento: String): CodigoAbordaje? = crear(argumento)

        private fun crear(patente: String): CodigoAbordaje? =
            if (PATENTE.matches(patente)) CodigoAbordaje(patente) else null
    }
}
