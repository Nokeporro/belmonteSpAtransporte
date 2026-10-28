package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VehiculosUiState(
    val moviles: List<Movil> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

class VehiculosViewModel @JvmOverloads constructor(
    application: Application,
    private val flota: FlotaRepository = FlotaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(VehiculosUiState())
    val estado: StateFlow<VehiculosUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var lista: List<Movil> = emptyList()
            val error = capturar(ErrorApp.Inesperado) { lista = flota.listarMoviles().valor() }
            _estado.update { it.copy(moviles = if (error == null) lista else it.moviles, cargando = false, error = error) }
        }
    }
}
