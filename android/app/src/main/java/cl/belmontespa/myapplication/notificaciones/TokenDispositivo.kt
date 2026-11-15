package cl.belmontespa.myapplication.notificaciones

interface TokenDispositivo {
    fun leerToken(): String?

    object Ninguno : TokenDispositivo {
        override fun leerToken(): String? = null
    }
}
