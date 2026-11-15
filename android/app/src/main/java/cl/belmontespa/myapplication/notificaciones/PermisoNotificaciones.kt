package cl.belmontespa.myapplication.notificaciones

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.repository.SoporteLocal

@Composable
fun PermisoNotificaciones(estado: EstadoSesion?) {
    val contexto = LocalContext.current
    val preferencias = remember(contexto) { PreferenciasNotificaciones(contexto) }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(estado) {
        if (estado !is EstadoSesion.Autenticada || !notificacionesRemotasHabilitadas()) return@LaunchedEffect
        NotificacionesFirebase.preparar(contexto)
        val concedido = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (debePedirPermisoNotificaciones(estado, SoporteLocal.modoLocalPorDefecto,
                BuildConfig.FIREBASE_CONFIGURADO, Build.VERSION.SDK_INT, concedido, preferencias.permisoSolicitado)) {
            preferencias.marcarPermisoSolicitado()
            permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
