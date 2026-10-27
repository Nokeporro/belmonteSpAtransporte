package cl.belmontespa.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Rol
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

data class CambiarPasswordUiState(
    val passwordActual: String = "",
    val passwordNueva: String = "",
    val confirmacion: String = "",
    /** La cuenta de la sesión; el Conductor no cambia su contraseña (la reemplaza el Administrador). */
    val cuenta: Cuenta? = null,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
)

/**
 * «Cambiar contraseña» y «Cambio obligatorio de contraseña» (HU22): la misma pantalla; la segunda se abre con la
 * contraseña temporal y no se puede saltar.
 */
@HiltViewModel
class CambiarPasswordViewModel @Inject constructor(
    private val cuentas: CuentaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(CambiarPasswordUiState())
    val estado: StateFlow<CambiarPasswordUiState> = _estado.asStateFlow()

    private var cargada = false

    /** Idempotente: la pantalla lo llama en cada composición. */
    fun cargar() {
        if (cargada) return
        cargada = true
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var cuenta: Cuenta? = null
            val error = capturar(ErrorApp.Inesperado) { cuenta = cuentas.obtenerPerfil().valor() }
            _estado.update { it.copy(cuenta = cuenta ?: it.cuenta, cargando = false, error = error) }
        }
    }

    fun reintentar() {
        cargada = false
        cargar()
    }

    private fun editar(cambio: CambiarPasswordUiState.() -> CambiarPasswordUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null, aviso = null) }

    fun escribirActual(texto: String) = editar { copy(passwordActual = texto) }
    fun escribirNueva(texto: String) = editar { copy(passwordNueva = texto) }
    fun escribirConfirmacion(texto: String) = editar { copy(confirmacion = texto) }

    fun guardar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val errores = Validaciones.cambioPassword(datos.passwordActual, datos.passwordNueva, datos.confirmacion)
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                if (datos.cuenta?.rol == Rol.CONDUCTOR) throw FalloPantalla(ErrorApp.SinPermiso())
                cuentas.cambiarPassword(datos.passwordActual, datos.passwordNueva).valor()
                // Las contraseñas no sobreviven a un envío exitoso.
                _estado.update {
                    it.copy(passwordActual = "", passwordNueva = "", confirmacion = "", aviso = R.string.aviso_password_guardada)
                }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }
}
