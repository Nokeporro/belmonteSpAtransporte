package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.permiteCambioA
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.PagoRepository
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.FalloPantalla
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetallePagoUiState(
    val pago: Pago? = null,
    val cuenta: Cuenta? = null,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
) {
    val esAdministrador: Boolean get() = cuenta?.rol == Rol.ADMINISTRADOR
}

class DetallePagoViewModel @JvmOverloads constructor(
    application: Application,
    private val pagos: PagoRepository = PagoRepository(application),
    private val cuentas: CuentaRepository = CuentaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(DetallePagoUiState())
    val estado: StateFlow<DetallePagoUiState> = _estado.asStateFlow()

    private var hash: String? = null
    private var consulta: Job? = null

    fun cargar(hash: String, forzar: Boolean = false) {
        if (hash == this.hash && !forzar) return
        this.hash = hash
        consulta?.cancel()
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, pago = null) }
            var pago: Pago? = null
            var cuenta: Cuenta? = null
            val error = capturar(ErrorApp.Inesperado) {
                cuenta = cuentas.obtenerPerfil().valor()
                pago = pagos.obtenerPago(hash).valor()
            }
            _estado.update { it.copy(cuenta = cuenta ?: it.cuenta, pago = pago, cargando = false, error = error) }
        }
    }

    fun reintentar() {
        hash?.let { cargar(it, forzar = true) }
    }

    fun alVolverAPrimerPlano() {
        if (!_estado.value.cargando) reintentar()
    }

    fun cambiarEstado(nuevo: EstadoPago) {
        val hash = hash ?: return
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val pago = datos.pago ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                if (!datos.esAdministrador) throw FalloPantalla(ErrorApp.SinPermiso())
                if (!pago.estado.permiteCambioA(nuevo)) {
                    throw FalloPantalla(ErrorApp.Conflicto("El cambio de pago no está permitido. Actualiza su información."))
                }
                val guardado = pagos.cambiarEstadoPago(hash, nuevo).valor()
                _estado.update { it.copy(pago = guardado, aviso = R.string.pago_estado_guardado) }
            }
            _estado.update { it.copy(enviando = false, error = error) }
        }
    }
}
