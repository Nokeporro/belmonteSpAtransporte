package cl.belmontespa.myapplication.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.PasswordTemporal
import cl.belmontespa.myapplication.repository.AuthRepository
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

data class RecuperarPasswordUiState(
    val correo: String = "",
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    /** La temporal generada: se muestra una sola vez, hasta que el Administrador cambie el correo o salga. */
    val temporal: PasswordTemporal? = null,
)

/** «Recuperar contraseña» (HU22): el Administrador genera la temporal de un Solicitante o Administrador. */
class RecuperarPasswordViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: AuthRepository = AuthRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(RecuperarPasswordUiState())
    val estado: StateFlow<RecuperarPasswordUiState> = _estado.asStateFlow()

    fun escribirCorreo(texto: String) =
        _estado.update { it.copy(correo = texto, modificado = true, error = null, temporal = null) }

    fun recuperar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, temporal = null) }
        viewModelScope.launch {
            val correo = _estado.value.correo
            val error = capturar(DATOS_INVALIDOS) {
                val errores = Validaciones.recuperarPassword(correo)
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                val temporal = auth.recuperarPassword(Validaciones.correo(correo)).valor()
                _estado.update { it.copy(temporal = temporal) }
            }
            _estado.update {
                it.copy(enviando = false, modificado = error != null && it.modificado, error = error.deCorreo())
            }
        }
    }

    /** Un Conductor o un correo desconocido no tienen temporal (404): se avisa en el campo. */
    private fun ErrorApp?.deCorreo(): ErrorApp? =
        if (this == ErrorApp.NoEncontrado) ErrorApp.Validacion(mapOf("correo" to SIN_CUENTA)) else this

    private companion object {
        const val SIN_CUENTA = "No hay un Solicitante ni un Administrador con ese correo."
    }
}
