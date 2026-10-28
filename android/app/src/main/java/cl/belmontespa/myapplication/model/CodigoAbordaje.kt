package cl.belmontespa.myapplication.model

data class CodigoAbordaje(
    val patente: String,
) {
    fun paraRuta(): String = patente

    companion object {
        private const val PREFIJO_QR = "BELMONTE-ABORDAJE"
        private val PATENTE = Regex("[A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2}")
        private val ESPACIOS_Y_GUIONES = Regex("[\\s-]")

        fun desdeQr(contenido: String): CodigoAbordaje? {
            val partes = contenido.uppercase().split(':')
            if (partes.size != 2 || partes[0] != PREFIJO_QR) return null
            return crear(partes[1])
        }

        fun manual(patente: String): CodigoAbordaje? = crear(patente.replace(ESPACIOS_Y_GUIONES, "").uppercase())

        fun desdeRuta(argumento: String): CodigoAbordaje? = crear(argumento)

        private fun crear(patente: String): CodigoAbordaje? =
            if (PATENTE.matches(patente)) CodigoAbordaje(patente) else null
    }
}
