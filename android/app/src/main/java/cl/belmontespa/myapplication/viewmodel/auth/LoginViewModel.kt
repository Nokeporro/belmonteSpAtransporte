package cl.belmontespa.myapplication.viewmodel.auth

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.MotivoSinPermiso
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** La contraseña vive solo en la memoria del ViewModel, nunca en `SavedStateHandle`. */
data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val enviando: Boolean = false,
    @param:StringRes val error: Int? = null,
)

class LoginViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(application),
) : AndroidViewModel(application) {

    /** En la build demo el Login avisa que los datos son ficticios. */
    val modoDemo: Boolean = BuildConfig.MODO_LOCAL

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarCorreo(correo: String) = _estado.update { it.copy(correo = correo, error = null) }

    fun cambiarPassword(password: String) = _estado.update { it.copy(password = password, error = null) }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val resultado = authRepository.iniciarSesion(actual.correo, actual.password)
            // Con éxito no hay nada que mostrar: la app navega al inicio del rol al cambiar la sesión.
            _estado.update {
                if (resultado is Resultado.Fallo) {
                    it.copy(enviando = false, error = mensajeLogin(resultado.error))
                } else {
                    it.copy(enviando = false, password = "")
                }
            }
        }
    }
}

/** Texto de un fallo del login; nunca dice si el correo existe. */
@StringRes
fun mensajeLogin(error: ErrorApp): Int = when (error) {
    // En el login, un 401 son credenciales inválidas (correo, contraseña o Conductor inactivo).
    ErrorApp.SesionExpirada -> R.string.error_credenciales
    is ErrorApp.SinPermiso -> when (error.motivo) {
        MotivoSinPermiso.CUENTA_PENDIENTE -> R.string.error_cuenta_pendiente
        MotivoSinPermiso.CUENTA_RECHAZADA -> R.string.error_cuenta_rechazada
        MotivoSinPermiso.CAMBIAR_PASSWORD -> R.string.error_debes_cambiar_password
        MotivoSinPermiso.GENERAL -> R.string.error_sin_permiso
    }
    ErrorApp.SinConexion -> R.string.error_sin_conexion
    is ErrorApp.Validacion -> R.string.error_revisa_datos
    ErrorApp.NoEncontrado -> R.string.error_no_encontrado
    is ErrorApp.Conflicto, ErrorApp.Inesperado -> R.string.error_inesperado
}
