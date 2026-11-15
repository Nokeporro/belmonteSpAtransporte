package cl.belmontespa.myapplication.notificaciones

import android.content.Context
import android.content.Intent
import cl.belmontespa.myapplication.MainActivity

enum class TipoAviso { VIAJE, AGENDA, CUENTAS }

data class AvisoPush(
    val tipo: TipoAviso?,
    val folio: String? = null,
    val titulo: String? = null,
    val mensaje: String? = null,
) {
    val etiqueta: String get() = folio ?: tipo?.name ?: SIN_TIPO

    fun aIntent(contexto: Context): Intent =
        Intent(contexto, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .also { intent ->
                tipo?.let { intent.putExtra(CLAVE_TIPO, it.name) }
                folio?.let { intent.putExtra(CLAVE_FOLIO, it) }
            }

    companion object {
        const val CLAVE_TIPO = "tipo"
        const val CLAVE_FOLIO = "folio"
        private const val SIN_TIPO = "AVISO"
        private val FOLIO = Regex("TR-\\d{1,9}")

        fun desde(tipo: String?, folio: String?, titulo: String? = null, mensaje: String? = null) = AvisoPush(
            tipo = TipoAviso.entries.firstOrNull { it.name == tipo?.trim()?.uppercase() },
            folio = folio?.trim()?.takeIf { FOLIO.matches(it) },
            titulo = titulo?.takeIf { it.isNotBlank() },
            mensaje = mensaje?.takeIf { it.isNotBlank() },
        )

        fun desdeIntent(intent: Intent?): AvisoPush? =
            desde(intent?.getStringExtra(CLAVE_TIPO), intent?.getStringExtra(CLAVE_FOLIO)).takeIf { it.tipo != null }
    }
}
