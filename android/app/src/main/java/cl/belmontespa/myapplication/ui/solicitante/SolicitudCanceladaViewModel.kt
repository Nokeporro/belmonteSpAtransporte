package cl.belmontespa.myapplication.ui.solicitante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.capturar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SolicitudCanceladaUiState(
    val viaje: Viaje? = null,
    val pago: Pago? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** «Solicitud cancelada» (HU16): el viaje cancelado y lo que pasó con su pago, leídos de lo guardado. */
@HiltViewModel
class SolicitudCanceladaViewModel @Inject constructor(
    private val viajes: ViajeRepository,
    private val pagos: PagoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(SolicitudCanceladaUiState())
    val estado: StateFlow<SolicitudCanceladaUiState> = _estado.asStateFlow()

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
}
