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

    // Solo llega con la app en primer plano: en segundo plano FCM muestra el aviso y lo abre con `data` como extras.
    override fun onMessageReceived(remoto: RemoteMessage) {
        if (!notificacionesRemotasHabilitadas()) return
        val aviso = AvisoPush.desde(
            tipo = remoto.data[AvisoPush.CLAVE_TIPO], folio = remoto.data[AvisoPush.CLAVE_FOLIO],
            titulo = remoto.notification?.title, mensaje = remoto.notification?.body,
        )
        AvisosEntrantes.publicar(aviso)
        PresentadorDeAvisos.presentar(this, aviso)
    }
}
