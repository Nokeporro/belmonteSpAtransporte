package cl.belmontespa.myapplication.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
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
    val confirmandoCierre: Boolean = false,
    val sincronizando: Boolean = false,
    val errorSincronizacion: ErrorApp? = null,
    val cerrar: Boolean = false,
)

class PerfilViewModel @JvmOverloads constructor(
    application: Application,
    private val cuentas: CuentaRepository = CuentaRepository(application),
    private val viajes: ViajeRepository = ViajeRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    fun cargar(forzar: Boolean = false) {
        if (!forzar && _estado.value.cuenta != null) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var cuenta: Cuenta? = null
            val error = capturar(ErrorApp.Inesperado) { cuenta = cuentas.obtenerPerfil().valor() }
            _estado.update { it.copy(cuenta = cuenta ?: it.cuenta, cargando = false, error = error) }
        }
    }

    fun pedirCerrarSesion() {
        if (_estado.value.sincronizando) return
        viewModelScope.launch {
            var pendientes = 0
            capturar(ErrorApp.Inesperado) { pendientes = viajes.contarPendientes() }
            _estado.update {
                if (pendientes > 0) it.copy(confirmandoCierre = true, errorSincronizacion = null) else it.copy(cerrar = true)
            }
        }
    }

    fun sincronizarYCerrar() {
        if (_estado.value.sincronizando) return
        _estado.update { it.copy(sincronizando = true, errorSincronizacion = null) }
        viewModelScope.launch {
            val resultado = viajes.sincronizarAhora()
            _estado.update {
                if (resultado is Resultado.Exito) {
                    it.copy(sincronizando = false, confirmandoCierre = false, cerrar = true)
                } else {
                    it.copy(sincronizando = false, errorSincronizacion = (resultado as? Resultado.Fallo)?.error ?: ErrorApp.Inesperado)
                }
            }
        }
    }

    fun descartarYCerrar() {
        if (_estado.value.sincronizando) return
        _estado.update { it.copy(confirmandoCierre = false, errorSincronizacion = null, cerrar = true) }
    }

    fun cancelarCierre() {
        if (_estado.value.sincronizando) return
        _estado.update { it.copy(confirmandoCierre = false, errorSincronizacion = null) }
    }

    fun consumirCierre() {
        _estado.update { it.copy(cerrar = false) }
    }
}
