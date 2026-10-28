package cl.belmontespa.myapplication.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificacionUiState(
    val cuenta: Cuenta? = null,
    val viajes: List<Viaje> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

class NotificacionViewModel @JvmOverloads constructor(
    application: Application,
    private val cuentas: CuentaRepository = CuentaRepository(application),
    private val viajes: ViajeRepository = ViajeRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(NotificacionUiState())
    val estado: StateFlow<NotificacionUiState> = _estado.asStateFlow()

    private var consulta: Job? = null

    fun cargar() {
        if (consulta?.isActive == true) return
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val error = capturar(ErrorApp.Inesperado) {
                val cuenta = cuentas.obtenerPerfil().valor()
                _estado.update { it.copy(cuenta = cuenta, cargando = false) }
                viajes.observarViajes(FiltroViajes()).collect { datos ->
                    _estado.update { it.copy(viajes = datos, cargando = false) }
                }
            }
            _estado.update { it.copy(cargando = false, error = error) }
        }
    }

    fun reintentar() {
        consulta?.cancel()
        cargar()
    }
}
