package cl.belmontespa.myapplication.notificaciones

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BelmonteFirebaseService : FirebaseMessagingService() {
    // HU14 y el backend usan el token de registro FCM, no el identificador de instalación (FID).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        if (!notificacionesRemotasHabilitadas()) return
        PreferenciasNotificaciones(this).guardarToken(token)
        TokenFcmWorker.programar(this)
    }

    override fun onMessageReceived(remoto: RemoteMessage) {
        if (!notificacionesRemotasHabilitadas()) return
        val datos = AvisoPush.deDatos(remoto.data) ?: return
        val aviso = datos.copy(titulo = remoto.notification?.title, mensaje = remoto.notification?.body)
        AvisosEntrantes.publicar(aviso)
        if (remoto.notification != null) PresentadorDeAvisos.presentar(this, aviso)
    }
}
