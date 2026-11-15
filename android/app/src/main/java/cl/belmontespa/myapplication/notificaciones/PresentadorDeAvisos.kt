package cl.belmontespa.myapplication.notificaciones

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.belmontespa.myapplication.R

object PresentadorDeAvisos {
    private const val ID = 1

    fun presentar(contexto: Context, aviso: AvisoPush) {
        val aplicacion = contexto.applicationContext
        if (ContextCompat.checkSelfPermission(aplicacion, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) return
        NotificacionesFirebase.crearCanal(aplicacion)
        val abrir = PendingIntent.getActivity(
            aplicacion, aviso.etiqueta.hashCode(), aviso.aIntent(aplicacion),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificacion = NotificationCompat.Builder(aplicacion, NotificacionesFirebase.CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_auto)
            .setContentTitle(aviso.titulo ?: aplicacion.getString(R.string.app_name))
            .setContentText(aviso.mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        NotificationManagerCompat.from(aplicacion).notify(aviso.etiqueta, ID, notificacion)
    }
}
