package cl.belmontespa.myapplication.viewmodel.solicitante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.viewmodel.capturar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MisSolicitudesUiState(
    val viajes: List<Viaje> = emptyList(),
    val estado: EstadoViaje? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

class MisSolicitudesViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(MisSolicitudesUiState())
    val estado: StateFlow<MisSolicitudesUiState> = _estado.asStateFlow()

    private var observacion: Job? = null

    fun cargar() {
        if (observacion == null) observar()
    }

    fun filtrar(estado: EstadoViaje?) {
        _estado.update { it.copy(estado = estado) }
        observar()
    }

    fun actualizar() = observar()

    private fun observar() {
        observacion?.cancel()
        observacion = viewModelScope.launch {
            _estado.update { it.copy(error = null) }
            val error = capturar(ErrorApp.Inesperado) {
                viajes.observarViajes(FiltroViajes(estado = _estado.value.estado)).collect { datos ->
                    _estado.update { it.copy(viajes = datos, cargando = false) }
                }
            }
            _estado.update { it.copy(cargando = false, error = error) }
        }
    }
}
