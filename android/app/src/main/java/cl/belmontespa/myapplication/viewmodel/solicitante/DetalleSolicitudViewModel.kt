package cl.belmontespa.myapplication.viewmodel.solicitante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.repository.PagoRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import cl.belmontespa.myapplication.viewmodel.capturar
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleSolicitudUiState(
    val viaje: Viaje? = null,
    val pago: Pago? = null,
    val cargandoPago: Boolean = false,
    val errorPago: ErrorApp? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/**
 * «Detalle de solicitud» del Solicitante, también cuando no tiene pasajeros (HU03, HU21): el viaje observado desde Room
 * y, si tiene pago, su estado. Un pago ilegible no oculta el viaje: se avisa y se permite reintentar.
 */
class DetalleSolicitudViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val pagos: PagoRepository = PagoRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(DetalleSolicitudUiState())
    val estado: StateFlow<DetalleSolicitudUiState> = _estado.asStateFlow()

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
                    // El viaje permanece visible mientras se consulta el pago, incluso si este falla.
                    _estado.update {
                        it.copy(
                            viaje = viaje, cargando = false, pago = null, errorPago = null,
                            cargandoPago = viaje?.hashPago != null,
                            error = if (viaje == null) ErrorApp.NoEncontrado else null,
                        )
                    }
                    val resultado = viaje?.hashPago?.let { hash ->
                        try {
                            pagos.obtenerPago(hash)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Resultado.Fallo(ErrorApp.Inesperado)
                        }
                    }
                    _estado.update {
                        it.copy(
                            pago = (resultado as? Resultado.Exito)?.valor,
                            cargandoPago = false,
                            errorPago = when (resultado) {
                                is Resultado.Fallo -> resultado.error
                                Resultado.Encolado -> ErrorApp.Inesperado
                                else -> null
                            },
                        )
                    }
                }
            }
            _estado.update { it.copy(cargando = false, error = error ?: it.error) }
        }
    }

    /** «Reintentar», «Reintentar pago» y la recarga tras un 409. */
    fun reintentar() {
        folio?.let { cargar(it, forzar = true) }
    }

    /** Al volver a primer plano (p. ej. desde el detalle del pago) se lee de nuevo el pago. */
    fun alVolverAPrimerPlano() {
        if (!_estado.value.cargando) reintentar()
    }

    fun ahora(): LocalDateTime = LocalDateTime.now(clock)
}
