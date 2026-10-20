package cl.belmontespa.myapplication.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.model.EstadoSesion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la sesión que decide el flujo de navegación de toda la app. */
@HiltViewModel
class SesionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** `null` mientras se lee la sesión guardada ("Comprobando sesión…"). */
    val estado: StateFlow<EstadoSesion?> = authRepository.observarSesion()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun cerrarSesion() {
        viewModelScope.launch { authRepository.cerrarSesion() }
    }
}
