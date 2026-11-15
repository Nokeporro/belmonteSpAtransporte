package cl.belmontespa.myapplication.notificaciones

import android.content.Context

class PreferenciasNotificaciones(contexto: Context, archivo: String = "notificaciones") : TokenDispositivo {
    private val preferencias = contexto.applicationContext.getSharedPreferences(archivo, Context.MODE_PRIVATE)

    override fun leerToken(): String? = preferencias.getString(TOKEN, null)?.takeIf { it.isNotBlank() }

    fun guardarToken(token: String) {
        if (token.isNotBlank()) preferencias.edit().putString(TOKEN, token).apply()
    }

    val permisoSolicitado: Boolean get() = preferencias.getBoolean(PERMISO_SOLICITADO, false)

    fun marcarPermisoSolicitado() {
        preferencias.edit().putBoolean(PERMISO_SOLICITADO, true).apply()
    }

    private companion object {
        const val TOKEN = "token_fcm"
        const val PERMISO_SOLICITADO = "permiso_solicitado"
    }
}
