package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevaCuenta
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

data class RegistrarConductorUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val password: String = "",
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    /** Correo del conductor guardado: la pantalla navega a su detalle y lo consume. */
    val registrado: String? = null,
)

/** «Registrar conductor» (HU15, RF14): nombre, correo único, teléfono opcional y la contraseña inicial. */
class RegistrarConductorViewModel @JvmOverloads constructor(
    application: Application,
    private val flota: FlotaRepository = FlotaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(RegistrarConductorUiState())
    val estado: StateFlow<RegistrarConductorUiState> = _estado.asStateFlow()

    private fun editar(cambio: RegistrarConductorUiState.() -> RegistrarConductorUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null) }

    fun escribirNombre(texto: String) = editar { copy(nombre = texto) }
    fun escribirCorreo(texto: String) = editar { copy(correo = texto) }
    fun escribirTelefono(texto: String) = editar { copy(telefono = texto) }
    fun escribirPassword(texto: String) = editar { copy(password = texto) }

    fun guardar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val cuenta = NuevaCuenta(datos.nombre, datos.correo, datos.telefono.ifBlank { null }, datos.password)
                val errores = Validaciones.conductor(cuenta)
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                val conductor = flota.registrarConductor(cuenta).valor()
                // La contraseña no sobrevive al envío exitoso.
                _estado.update { it.copy(password = "", registrado = conductor.correo) }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }

    fun consumirRegistrado() = _estado.update { it.copy(registrado = null) }
}
