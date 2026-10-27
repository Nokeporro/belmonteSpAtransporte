package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConductoresUiState(
    val conductores: List<Conductor> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** «Conductores» (HU15): la lista con el estado y los viajes asignados de cada uno. */
class ConductoresViewModel @JvmOverloads constructor(
    application: Application,
    private val flota: FlotaRepository = FlotaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(ConductoresUiState())
    val estado: StateFlow<ConductoresUiState> = _estado.asStateFlow()

    /** Se llama al abrir y al volver del alta o de editar, para leer lo guardado. */
    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var lista: List<Conductor> = emptyList()
            val error = capturar(ErrorApp.Inesperado) { lista = flota.listarConductores().valor() }
            _estado.update { it.copy(conductores = if (error == null) lista else it.conductores, cargando = false, error = error) }
        }
    }
}
