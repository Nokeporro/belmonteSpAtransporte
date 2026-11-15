package cl.belmontespa.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.navigation.BelmonteApp
import cl.belmontespa.myapplication.notificaciones.AvisoPush
import cl.belmontespa.myapplication.notificaciones.AvisosEntrantes
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.viewmodel.SesionViewModel
import cl.belmontespa.myapplication.notificaciones.PermisoNotificaciones

class MainActivity : ComponentActivity() {

    private val sesionViewModel: SesionViewModel by viewModels()

    private var aviso by mutableStateOf<AvisoPush?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) aviso = AvisoPush.desdeIntent(intent)
        setContent {
            BelmonteTheme {
                val estadoSesion by sesionViewModel.estado.collectAsStateWithLifecycle()
                PermisoNotificaciones(estadoSesion)
                BelmonteApp(
                    estadoSesion = estadoSesion,
                    onCerrarSesion = sesionViewModel::cerrarSesion,
                    aviso = aviso,
                    onAvisoAtendido = { aviso = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AvisoPush.desdeIntent(intent)?.let {
            aviso = it
            AvisosEntrantes.publicar(it)
        }
    }
}
