package cl.belmontespa.myapplication.notificaciones

import com.google.firebase.messaging.FirebaseMessagingService

class BelmonteFirebaseService : FirebaseMessagingService() {
    // HU14 y el backend usan el token de registro FCM, no el identificador de instalación (FID).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        if (!notificacionesRemotasHabilitadas()) return
        PreferenciasNotificaciones(this).guardarToken(token)
        TokenFcmWorker.programar(this)
    }
}
