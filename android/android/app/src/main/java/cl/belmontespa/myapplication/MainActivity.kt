package cl.belmontespa.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.ui.navigation.BelmonteApp
import cl.belmontespa.myapplication.ui.navigation.SesionViewModel
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import dagger.hilt.android.AndroidEntryPoint

/** Única actividad: dibuja la app en Compose y decide el flujo según la sesión. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sesionViewModel: SesionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BelmonteTheme {
                val estadoSesion by sesionViewModel.estado.collectAsStateWithLifecycle()
                BelmonteApp(estadoSesion = estadoSesion, onCerrarSesion = sesionViewModel::cerrarSesion)
            }
        }
    }
}
