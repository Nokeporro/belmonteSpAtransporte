package cl.belmontespa.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la sesión que decide el flujo de navegación de toda la app. */
class SesionViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(application),
) : AndroidViewModel(application) {

    /** `null` mientras se lee la sesión guardada ("Comprobando sesión…"). */
    val estado: StateFlow<EstadoSesion?> = authRepository.observarSesion()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun cerrarSesion() {
        viewModelScope.launch { authRepository.cerrarSesion() }
    }
}
