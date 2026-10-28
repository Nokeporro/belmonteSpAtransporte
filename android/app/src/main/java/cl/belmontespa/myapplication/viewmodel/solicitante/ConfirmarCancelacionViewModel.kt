package cl.belmontespa.myapplication.viewmodel.solicitante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.cancelablePorSolicitante
import cl.belmontespa.myapplication.repository.PagoRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.FalloPantalla
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfirmarCancelacionUiState(
    val viaje: Viaje? = null,
    val pago: Pago? = null,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val cancelada: Boolean = false,
)

class ConfirmarCancelacionViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val pagos: PagoRepository = PagoRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(ConfirmarCancelacionUiState())
    val estado: StateFlow<ConfirmarCancelacionUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var consulta: Job? = null

    fun cargar(folio: String, forzar: Boolean = false) {
        if (folio == this.folio && !forzar) return
        this.folio = folio
        consulta?.cancel()
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val error = capturar(ErrorApp.Inesperado) {
                _estado.update { it.copy(cargando = false) }
                viajes.observarConPago(folio, pagos).collect { (viaje, pago) ->
                    _estado.update {
                        it.copy(
                            viaje = viaje, pago = pago, cargando = false,
                            error = if (viaje == null) ErrorApp.NoEncontrado else null,
                        )
                    }
                }
            }
            _estado.update { it.copy(cargando = false, error = error ?: it.error) }
        }
    }

    fun reintentar() {
        folio?.let { cargar(it, forzar = true) }
    }

    fun ahora(): LocalDateTime = LocalDateTime.now(clock)

    fun confirmar() {
        val folio = folio ?: return
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val error = capturar(DATOS_INVALIDOS) {
                val viaje = _estado.value.viaje ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                if (!viaje.cancelablePorSolicitante(ahora())) {
                    throw FalloPantalla(ErrorApp.Conflicto("La solicitud ya no se puede cancelar. Actualiza su información."))
                }
                viajes.cancelar(folio).valor()
                _estado.update { it.copy(cancelada = true) }
            }
            _estado.update { it.copy(enviando = false, error = error) }
        }
    }
}
