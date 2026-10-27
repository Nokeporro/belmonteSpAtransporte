package cl.belmontespa.myapplication.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
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
    val aviso: Int? = null,
) {
    /** La confirmación es genérica: no dice si el correo existe (CU-13). */
    val enviado: Boolean get() = aviso == R.string.aviso_recuperacion_enviada
}

/** «Recuperar contraseña» (HU22): pide el correo y confirma sin revelar si la cuenta existe. */
class RecuperarPasswordViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: AuthRepository = AuthRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(RecuperarPasswordUiState())
    val estado: StateFlow<RecuperarPasswordUiState> = _estado.asStateFlow()

    fun escribirCorreo(texto: String) =
        _estado.update { it.copy(correo = texto, modificado = true, error = null, aviso = null) }

    fun recuperar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val correo = _estado.value.correo
            val error = capturar(DATOS_INVALIDOS) {
                val errores = Validaciones.recuperarPassword(correo)
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                auth.recuperarPassword(Validaciones.correo(correo)).valor()
                _estado.update { it.copy(aviso = R.string.aviso_recuperacion_enviada) }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }
}
