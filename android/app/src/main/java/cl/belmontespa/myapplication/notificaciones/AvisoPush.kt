package cl.belmontespa.myapplication.notificaciones

import android.content.Context
import android.content.Intent
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.navigation.Destino
import cl.belmontespa.myapplication.navigation.PantallaDemo

enum class TipoAviso { VIAJE, AGENDA, CUENTAS }

data class AvisoPush(
    val tipo: TipoAviso?,
    val folio: String? = null,
    val titulo: String? = null,
    val mensaje: String? = null,
) {
    val etiqueta: String get() = folio ?: tipo?.name ?: "AVISO"

    fun aIntent(contexto: Context): Intent = AvisosAndroid.intent(contexto, this)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun destino(estado: EstadoSesion?): Destino? =
        (estado as? EstadoSesion.Autenticada)?.sesion?.rol?.let { destino(it) }

    // El payload solo decide la pantalla. El Repository consulta con el JWT actual.
    fun destino(rol: Rol): Destino? = when (tipo) {
        TipoAviso.VIAJE -> folio?.takeIf { FOLIO.matches(it) }?.let {
            Destino(when (rol) {
                Rol.SOLICITANTE -> PantallaDemo.DETALLE_SOLICITUD
                Rol.ADMINISTRADOR -> PantallaDemo.DETALLE_ADMIN
                Rol.CONDUCTOR -> PantallaDemo.DETALLE_VIAJE
            }, it)
        }
        TipoAviso.AGENDA -> if (rol == Rol.CONDUCTOR) Destino(PantallaDemo.MIS_VIAJES) else null
        TipoAviso.CUENTAS -> if (rol == Rol.ADMINISTRADOR) Destino(PantallaDemo.CUENTAS_PENDIENTES) else null
        null -> null
    }

    companion object {
        const val CLAVE_TIPO = "tipo"
        const val CLAVE_FOLIO = "folio"
        private val FOLIO = Regex("^TR-[1-9][0-9]{3,16}$")

        fun desde(tipo: String?, folio: String?, titulo: String? = null, mensaje: String? = null) = AvisoPush(
            tipo = TipoAviso.entries.firstOrNull { it.name == tipo?.trim()?.uppercase() },
            folio = folio?.trim()?.takeIf { FOLIO.matches(it) },
            titulo = titulo?.takeIf { it.isNotBlank() },
            mensaje = mensaje?.takeIf { it.isNotBlank() },
        )

        fun deDatos(datos: Map<String, String>): AvisoPush? =
            desde(datos[CLAVE_TIPO], datos[CLAVE_FOLIO]).takeIf {
                it.tipo != null && (it.tipo != TipoAviso.VIAJE || it.folio != null)
            }

        fun desdeIntent(intent: Intent?): AvisoPush? = deDatos(mapOf(
            CLAVE_TIPO to intent?.getStringExtra(CLAVE_TIPO).orEmpty(),
            CLAVE_FOLIO to intent?.getStringExtra(CLAVE_FOLIO).orEmpty(),
        ))
    }
}
