package cl.belmontespa.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.repository.AuthRepository
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SesionViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow<EstadoSesion?>(null)

    val estado: StateFlow<EstadoSesion?> = _estado.asStateFlow()

    private var vencimiento: Job? = null

    init {
        viewModelScope.launch {
            authRepository.observarSesion().collect { nuevo ->
                _estado.value = nuevo
                esperarVencimiento(nuevo)
            }
        }
    }

    private fun esperarVencimiento(estado: EstadoSesion) {
        vencimiento?.cancel()
        val sesion = when (estado) {
            is EstadoSesion.Autenticada -> estado.sesion
            is EstadoSesion.Temporal -> estado.sesion
            else -> return
        }
        vencimiento = viewModelScope.launch {
            delay(sesion.expiraEn - clock.millis())
            _estado.value = EstadoSesion.SinSesion
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch { authRepository.cerrarSesion() }
    }
}
