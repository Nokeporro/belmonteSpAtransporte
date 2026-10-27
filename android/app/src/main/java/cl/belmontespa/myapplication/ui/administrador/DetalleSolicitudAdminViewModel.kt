package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.capturar
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleSolicitudAdminUiState(
    val viaje: Viaje? = null,
    /** El pago del viaje, si se pudo leer; un pago ilegible no le oculta el viaje al Administrador (HU24). */
    val pago: Pago? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** «Detalle de solicitud» del Administrador, también cuando no tiene pasajeros (HU04, HU05, HU24, HU30). */
@HiltViewModel
class DetalleSolicitudAdminViewModel @Inject constructor(
    private val viajes: ViajeRepository,
    private val pagos: PagoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleSolicitudAdminUiState())
    val estado: StateFlow<DetalleSolicitudAdminUiState> = _estado.asStateFlow()

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
                viajes.observarViaje(folio).collectLatest { viaje ->
                    val pago = viaje?.hashPago?.let { hash -> (pagos.obtenerPago(hash) as? Resultado.Exito)?.valor }
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
}
