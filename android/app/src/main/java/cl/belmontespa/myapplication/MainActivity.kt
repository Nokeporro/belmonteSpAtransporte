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
import cl.belmontespa.myapplication.notificaciones.AvisosAndroid
import cl.belmontespa.myapplication.notificaciones.AvisosEntrantes
import cl.belmontespa.myapplication.notificaciones.PermisoNotificaciones
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.viewmodel.SesionViewModel

class MainActivity : ComponentActivity() {

    private val sesionViewModel: SesionViewModel by viewModels()
    private var avisoPendiente by mutableStateOf<AvisoPush?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        avisoPendiente = if (savedInstanceState != null) AvisoPush.deDatos(mapOf(
            "tipo" to savedInstanceState.getString("aviso_tipo").orEmpty(),
            "folio" to savedInstanceState.getString("aviso_folio").orEmpty(),
        )) else AvisosAndroid.leer(intent)
        limpiarAvisoDelIntent()
        enableEdgeToEdge()
        setContent {
            BelmonteTheme {
                val estadoSesion by sesionViewModel.estado.collectAsStateWithLifecycle()
                PermisoNotificaciones(estadoSesion)
                BelmonteApp(estadoSesion = estadoSesion, onCerrarSesion = sesionViewModel::cerrarSesion,
                    aviso = avisoPendiente, onAvisoAtendido = { avisoPendiente = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AvisosAndroid.leer(intent)?.let {
            avisoPendiente = it
            AvisosEntrantes.publicar(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        avisoPendiente?.let {
            outState.putString("aviso_tipo", it.tipo?.name)
            outState.putString("aviso_folio", it.folio)
        }
        super.onSaveInstanceState(outState)
    }

    private fun limpiarAvisoDelIntent() {
        intent.removeExtra("tipo")
        intent.removeExtra("folio")
    }
}
