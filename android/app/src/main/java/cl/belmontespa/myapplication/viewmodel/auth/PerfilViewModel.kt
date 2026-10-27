package cl.belmontespa.myapplication.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.cuenta
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilUiState(
    val cuenta: Cuenta? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** «Perfil» de los tres roles: los datos de la cuenta de la sesión; los enlaces cambian según el rol. */
class PerfilViewModel @JvmOverloads constructor(
    application: Application,
    private val cuentas: CuentaRepository = CuentaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    /** Idempotente; «Reintentar» pasa [forzar]. */
    fun cargar(forzar: Boolean = false) {
        if (!forzar && _estado.value.cuenta != null) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var cuenta: Cuenta? = null
            val error = capturar(ErrorApp.Inesperado) { cuenta = cuentas.obtenerPerfil().valor() }
            _estado.update { it.copy(cuenta = cuenta ?: it.cuenta, cargando = false, error = error) }
        }
    }
}
