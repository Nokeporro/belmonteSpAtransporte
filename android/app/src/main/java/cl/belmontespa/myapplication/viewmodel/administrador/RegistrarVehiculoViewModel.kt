package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.util.Validaciones
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.FalloPantalla
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarVehiculoUiState(
    val patente: String = "",
    val capacidad: String = "",
    val gps: String = "",
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    val registrado: String? = null,
)

class RegistrarVehiculoViewModel @JvmOverloads constructor(
    application: Application,
    private val flota: FlotaRepository = FlotaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(RegistrarVehiculoUiState())
    val estado: StateFlow<RegistrarVehiculoUiState> = _estado.asStateFlow()

    private fun editar(cambio: RegistrarVehiculoUiState.() -> RegistrarVehiculoUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null) }

    fun escribirPatente(texto: String) = editar { copy(patente = texto) }
    fun escribirCapacidad(texto: String) = editar { copy(capacidad = texto) }
    fun escribirGps(texto: String) = editar { copy(gps = texto) }

    fun guardar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val gps = datos.gps.trim()
                val nuevo = NuevoMovil(
                    Validaciones.patente(datos.patente),
                    datos.capacidad.trim().toIntOrNull() ?: 0,
                    if (gps.isEmpty()) null else gps.toLongOrNull() ?: 0L,
                )
                val errores = Validaciones.movil(nuevo)
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                val movil = flota.registrarMovil(nuevo).valor()
                _estado.update { it.copy(registrado = movil.patente) }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }

    fun consumirRegistrado() = _estado.update { it.copy(registrado = null) }
}
