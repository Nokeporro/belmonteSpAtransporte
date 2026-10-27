package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.ui.capturar
import cl.belmontespa.myapplication.ui.valor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

/** «Vehículos» (HU17): la lista con la capacidad y el estado de cada uno. */
@HiltViewModel
class VehiculosViewModel @Inject constructor(
    private val flota: FlotaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(VehiculosUiState())
    val estado: StateFlow<VehiculosUiState> = _estado.asStateFlow()

    /** Se llama al abrir y al volver del alta o de editar, para leer lo guardado. */
    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var lista: List<Movil> = emptyList()
            val error = capturar(ErrorApp.Inesperado) { lista = flota.listarMoviles().valor() }
            _estado.update { it.copy(moviles = if (error == null) lista else it.moviles, cargando = false, error = error) }
        }
    }
}
