package cl.belmontespa.myapplication.ui.solicitante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.cancelablePorSolicitante
import cl.belmontespa.myapplication.ui.DATOS_INVALIDOS
import cl.belmontespa.myapplication.ui.FalloPantalla
import cl.belmontespa.myapplication.ui.capturar
import cl.belmontespa.myapplication.ui.valor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
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
    /** Verdadero cuando la cancelación quedó guardada: la pantalla navega a «Solicitud cancelada». */
    val cancelada: Boolean = false,
)

/**
 * «Confirmar cancelación» del Solicitante (HU16, V5): muestra el efecto sobre el pago antes de confirmar y, al
 * confirmar, cancela una sola vez aunque se toque dos veces.
 */
@HiltViewModel
class ConfirmarCancelacionViewModel @Inject constructor(
    private val viajes: ViajeRepository,
    private val pagos: PagoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _estado = MutableStateFlow(ConfirmarCancelacionUiState())
    val estado: StateFlow<ConfirmarCancelacionUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var consulta: Job? = null

    /** Idempotente con el mismo folio; [forzar] vuelve a leerlo. */
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
