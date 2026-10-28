package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.retraso
import cl.belmontespa.myapplication.repository.PagoRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import cl.belmontespa.myapplication.util.Validaciones
import java.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val CAMPO_MOTIVO = "motivo"
internal const val CAMPO_REEMBOLSO = "montoReembolso"

internal const val MONTO_POR_CONFIRMAR = "El viaje venció: confirma el monto a reembolsar."

data class CancelarViajeUiState(
    val cargando: Boolean = true,
    val viaje: Viaje? = null,
    val pago: Pago? = null,
    val motivo: String = "",
    val reembolso: String = "",
    val vencido: Boolean = false,
    val enviando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: ErrorApp? = null,
    val cancelado: Boolean = false,
) {
    val cancelable: Boolean get() = viaje?.estado == EstadoViaje.PENDIENTE || viaje?.estado == EstadoViaje.ASIGNADO

    val pideReembolso: Boolean get() = vencido && pago?.estado == EstadoPago.PAGADO

    val modificado: Boolean get() = !cancelado && motivo.isNotBlank()

    val pagoIlegible: Boolean get() = viaje?.hashPago != null && pago == null

    val puedeConfirmar: Boolean get() = viaje != null && cancelable && !enviando && !cancelado && !pagoIlegible
}

class CancelarViajeViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val pagos: PagoRepository = PagoRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(CancelarViajeUiState())
    val estado: StateFlow<CancelarViajeUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var observacion: Job? = null

    private var reembolsoTocado = false

    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        reembolsoTocado = false
        _estado.value = CancelarViajeUiState()
        recargar()
    }

    fun reintentar() {
        _estado.update { it.copy(error = null) }
        recargar()
    }

    fun escribirMotivo(texto: String) {
        if (_estado.value.enviando) return
        _estado.update { it.copy(motivo = texto, errores = it.errores - CAMPO_MOTIVO, error = it.errorQueSigue()) }
    }

    fun escribirReembolso(texto: String) {
        if (_estado.value.enviando) return
        reembolsoTocado = true
        _estado.update { it.copy(reembolso = texto, errores = it.errores - CAMPO_REEMBOLSO, error = it.errorQueSigue()) }
    }

    private fun CancelarViajeUiState.errorQueSigue(): ErrorApp? = error.takeIf { pagoIlegible }

    fun confirmar() {
        val folio = folio ?: return
        val estado = _estado.value
        val viaje = estado.viaje ?: return
        if (!estado.puedeConfirmar) return
        val vencido = viaje.retraso(clock) == Retraso.VENCIDO
        val pago = estado.pago
        val pideReembolso = vencido && pago?.estado == EstadoPago.PAGADO
        val errores = buildMap {
            Validaciones.motivo(estado.motivo)?.let { put(CAMPO_MOTIVO, it) }
            if (pideReembolso) Validaciones.reembolso(estado.reembolso, pago.monto)?.let { put(CAMPO_REEMBOLSO, it) }
            if (pideReembolso && !estado.pideReembolso && CAMPO_REEMBOLSO !in this) put(CAMPO_REEMBOLSO, MONTO_POR_CONFIRMAR)
        }
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(vencido = vencido, errores = errores, error = null) }
            return
        }
        _estado.update { it.copy(vencido = vencido, enviando = true, errores = emptyMap(), error = null) }
        val monto = if (pideReembolso) estado.reembolso.trim().toInt() else null
        viewModelScope.launch {
            when (val resultado = viajes.cancelar(folio, estado.motivo.trim(), monto)) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, cancelado = true) }
                is Resultado.Fallo -> {
                    fallo(resultado.error)
                    if (resultado.error is ErrorApp.Conflicto) recargar(conservar = true)
                }
                Resultado.Encolado -> _estado.update { it.copy(enviando = false, error = ErrorApp.Inesperado) }
            }
        }
    }

    private fun fallo(error: ErrorApp) {
        if (error !is ErrorApp.Validacion) {
            _estado.update { it.copy(enviando = false, error = error) }
            return
        }
        val deCampos = error.errores.filterKeys { it == CAMPO_MOTIVO || it == CAMPO_REEMBOLSO }
        val resto = error.errores - deCampos.keys
        _estado.update {
            it.copy(enviando = false, errores = deCampos, error = ErrorApp.Validacion(resto).takeIf { resto.isNotEmpty() })
        }
    }

    private fun recargar(conservar: Boolean = false) {
        val folio = folio ?: return
        observacion?.cancel()
        _estado.update { if (conservar) it else it.copy(cargando = true) }
        observacion = viewModelScope.launch {
            val refresco = viajes.refrescarViaje(folio)
            if (refresco is Resultado.Fallo && refresco.error != ErrorApp.SinConexion) {
                _estado.update { it.copy(cargando = false, error = it.error ?: refresco.error) }
                return@launch
            }
            viajes.observarViaje(folio)
                .catch { _estado.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
                .collect { viaje ->
                    if (viaje == null) {
                        _estado.update { it.copy(cargando = false, error = ErrorApp.NoEncontrado) }
                    } else {
                        val pago = pagoDe(viaje)
                        _estado.update {
                            it.copy(
                                cargando = false, viaje = viaje, pago = pago,
                                vencido = viaje.retraso(clock) == Retraso.VENCIDO,
                                reembolso = if (!reembolsoTocado && pago != null) pago.monto.toString() else it.reembolso,
                            )
                        }
                    }
                }
        }
    }

    private suspend fun pagoDe(viaje: Viaje): Pago? {
        val hash = viaje.hashPago ?: return null
        return when (val resultado = pagos.obtenerPago(hash)) {
            is Resultado.Exito -> resultado.valor
            is Resultado.Fallo -> {
                _estado.update { it.copy(error = it.error ?: resultado.error) }
                null
            }
            Resultado.Encolado -> null
        }
    }
}
