package cl.belmontespa.myapplication.model

data class CodigoAbordaje(
    val patente: String,
    val codigo: String,
) {
    fun paraRuta(): String = "$patente:$codigo"

    companion object {
        private const val PREFIJO_QR = "BELMONTE-ABORDAJE"
        private val PATENTE = Regex("[A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2}")
        private val CODIGO = Regex("[0-9A-F]{16}")
        private val ESPACIOS_Y_GUIONES = Regex("[\\s-]")
        private val ESPACIOS = Regex("\\s")

        fun desdeQr(contenido: String): CodigoAbordaje? {
            val partes = contenido.uppercase().split(':')
            if (partes.size != 3 || partes[0] != PREFIJO_QR) return null
            return crear(partes[1], partes[2])
        }

        fun manual(patente: String, codigo: String): CodigoAbordaje? =
            crear(patente.replace(ESPACIOS_Y_GUIONES, "").uppercase(), codigo.replace(ESPACIOS, "").uppercase())

        fun desdeRuta(argumento: String): CodigoAbordaje? {
            val partes = argumento.split(':')
            return if (partes.size == 2) crear(partes[0], partes[1]) else null
        }

        private fun crear(patente: String, codigo: String): CodigoAbordaje? =
            if (PATENTE.matches(patente) && CODIGO.matches(codigo)) CodigoAbordaje(patente, codigo) else null
    }
}
