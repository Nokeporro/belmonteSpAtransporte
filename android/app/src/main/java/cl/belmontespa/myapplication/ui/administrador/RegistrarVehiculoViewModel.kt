package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.ui.DATOS_INVALIDOS
import cl.belmontespa.myapplication.ui.FalloPantalla
import cl.belmontespa.myapplication.ui.capturar
import cl.belmontespa.myapplication.ui.valor
import cl.belmontespa.myapplication.util.Validaciones
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    /** Patente del vehículo guardado: la pantalla navega a su detalle y la consume. */
    val registrado: String? = null,
)

/** «Registrar vehículo» (HU17, RF16): patente única, capacidad de 1 a 20 y serial GPS opcional. */
@HiltViewModel
class RegistrarVehiculoViewModel @Inject constructor(
    private val flota: FlotaRepository,
) : ViewModel() {

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
                // Un GPS ilegible no se confunde con «sin GPS»: llega como 0 y la validación lo rechaza.
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
