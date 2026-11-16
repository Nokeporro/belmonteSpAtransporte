package cl.belmontespa.myapplication.notificaciones

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

object NotificacionesFirebase {
    const val CANAL_AVISOS = "avisos"

    @Suppress("DEPRECATION") // Mantiene el contrato tokenFcm del backend y de onNewToken.
    fun preparar(contexto: Context) {
        if (!notificacionesRemotasHabilitadas()) return
        val aplicacion = contexto.applicationContext
        if (FirebaseApp.getApps(aplicacion).isEmpty() && FirebaseApp.initializeApp(aplicacion) == null) return
        crearCanal(aplicacion)
        FirebaseMessaging.getInstance().apply {
            isAutoInitEnabled = true
            token.addOnSuccessListener { actual ->
                PreferenciasNotificaciones(aplicacion).guardarToken(actual)
                TokenFcmWorker.programar(aplicacion)
            }
        }
    }

    fun crearCanal(contexto: Context) = AvisosAndroid.crearCanal(contexto)
}
