package cl.belmontespa.myapplication.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import cl.belmontespa.myapplication.R
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

    fun crearCanal(contexto: Context) {
        val aplicacion = contexto.applicationContext
        aplicacion.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CANAL_AVISOS, aplicacion.getString(R.string.canal_avisos_nombre), NotificationManager.IMPORTANCE_HIGH),
        )
    }
}
