package cl.belmontespa.myapplication.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R

object AvisosAndroid {
    fun crearCanal(contexto: Context) {
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(NotificacionesFirebase.CANAL_AVISOS,
                contexto.getString(R.string.canal_avisos_nombre), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    fun leer(intent: Intent?): AvisoPush? = AvisoPush.deDatos(mapOf(
        "tipo" to intent?.getStringExtra("tipo").orEmpty(),
        "folio" to intent?.getStringExtra("folio").orEmpty(),
    ))

    fun intent(contexto: Context, aviso: AvisoPush): Intent = Intent(contexto, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        // La identidad distingue los PendingIntent de viajes distintos, aunque compartan requestCode.
        data = Uri.Builder().scheme("belmonte").authority("aviso")
            .appendPath(aviso.tipo?.name.orEmpty()).appendPath(aviso.folio.orEmpty()).build()
        putExtra("tipo", aviso.tipo?.name)
        putExtra("folio", aviso.folio)
    }

    fun mostrar(contexto: Context, aviso: AvisoPush, titulo: String, mensaje: String): Boolean {
        if (AvisoPush.deDatos(mapOf("tipo" to aviso.tipo?.name.orEmpty(),
                "folio" to aviso.folio.orEmpty())) == null) return false
        crearCanal(contexto)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(contexto,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = contexto.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        val accion = PendingIntent.getActivity(contexto, 0, intent(contexto, aviso),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notificacion = NotificationCompat.Builder(contexto, NotificacionesFirebase.CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(accion)
            .build()
        try {
            manager.notify("${aviso.tipo}:${aviso.folio}", 0, notificacion)
        } catch (_: SecurityException) {
            // El permiso puede revocarse entre la comprobación y la publicación.
            return false
        }
        return true
    }
}
