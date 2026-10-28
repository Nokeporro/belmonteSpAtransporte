package cl.belmontespa.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.navigation.BelmonteApp
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.viewmodel.SesionViewModel

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
