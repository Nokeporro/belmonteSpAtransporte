package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.retraso
import cl.belmontespa.myapplication.util.Validaciones
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Los campos del formulario, con el nombre del contrato JSON que usan los errores de validación (400). */
internal const val CAMPO_MOTIVO = "motivo"
internal const val CAMPO_REEMBOLSO = "montoReembolso"

/** Bajo el monto cuando el viaje venció con la pantalla abierta y el campo aún no se había mostrado. */
internal const val MONTO_POR_CONFIRMAR = "El viaje venció: confirma el monto a reembolsar."

data class CancelarViajeUiState(
    val cargando: Boolean = true,
    val viaje: Viaje? = null,
    val pago: Pago? = null,
    val motivo: String = "",
    val reembolso: String = "",
    /** Viaje `PENDIENTE` o `ASIGNADO` con más de una hora de atraso: el reembolso lo decide el Administrador. */
    val vencido: Boolean = false,
    val enviando: Boolean = false,
    /** Motivo del error de validación bajo cada campo (`motivo`, `montoReembolso`). */
    val errores: Map<String, String> = emptyMap(),
    val error: ErrorApp? = null,
    /** Verdadero cuando la cancelación quedó guardada: la pantalla navega al detalle. */
    val cancelado: Boolean = false,
) {
    /** V7 solo cancela un viaje `PENDIENTE` o `ASIGNADO`. */
    val cancelable: Boolean get() = viaje?.estado == EstadoViaje.PENDIENTE || viaje?.estado == EstadoViaje.ASIGNADO

    /** El monto se pide solo con el viaje vencido y el pago `PAGADO`; si no, se reembolsa el total sin preguntar. */
    val pideReembolso: Boolean get() = vencido && pago?.estado == EstadoPago.PAGADO

    /** Hay un motivo escrito que se perdería al salir. */
    val modificado: Boolean get() = !cancelado && motivo.isNotBlank()

    /** El viaje tiene pago pero no se pudo leer: no se confirma, porque no se sabría qué se reembolsa. */
    val pagoIlegible: Boolean get() = viaje?.hashPago != null && pago == null

    val puedeConfirmar: Boolean get() = viaje != null && cancelable && !enviando && !cancelado && !pagoIlegible
}

/**
 * «Cancelar viaje» del Administrador (HU24, V7): motivo y, con el viaje vencido y el pago `PAGADO`, el monto a
 * reembolsar, precargado con el total. Si el viaje no está vencido el pago se reembolsa completo sin preguntar.
 */
@HiltViewModel
class CancelarViajeViewModel @Inject constructor(
    private val viajes: ViajeRepository,
    private val pagos: PagoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _estado = MutableStateFlow(CancelarViajeUiState())
    val estado: StateFlow<CancelarViajeUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var observacion: Job? = null

    // El monto precargado no pisa lo que el Administrador ya escribió.
    private var reembolsoTocado = false

    /** Idempotente: la pantalla lo llama en cada composición con el mismo folio. */
    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        reembolsoTocado = false
        _estado.value = CancelarViajeUiState()
        recargar()
    }

    /** «Reintentar» y la recarga tras un 409: vuelve a leer el viaje y su pago. */
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

    // Escribir borra el error del último intento, pero no el de un pago que sigue sin poder leerse.
    private fun CancelarViajeUiState.errorQueSigue(): ErrorApp? = error.takeIf { pagoIlegible }

    fun confirmar() {
        val folio = folio ?: return
        val estado = _estado.value
        val viaje = estado.viaje ?: return
        if (!estado.puedeConfirmar) return
        // El viaje pudo vencer mientras se miraba esta pantalla: manda la hora de ahora, no la de la última lectura.
        val vencido = viaje.retraso(clock) == Retraso.VENCIDO
        val pago = estado.pago
        val pideReembolso = vencido && pago?.estado == EstadoPago.PAGADO
        val errores = buildMap {
            Validaciones.motivo(estado.motivo)?.let { put(CAMPO_MOTIVO, it) }
            if (pideReembolso) Validaciones.reembolso(estado.reembolso, pago.monto)?.let { put(CAMPO_REEMBOLSO, it) }
            // Vencido el viaje el monto lo decide el Administrador (V7): si el campo no estaba a la vista, primero se muestra.
            if (pideReembolso && !estado.pideReembolso && CAMPO_REEMBOLSO !in this) put(CAMPO_REEMBOLSO, MONTO_POR_CONFIRMAR)
        }
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(vencido = vencido, errores = errores, error = null) }
            return
        }
        // `enviando` se marca antes de lanzar nada: un segundo toque no encuentra el botón libre.
        _estado.update { it.copy(vencido = vencido, enviando = true, errores = emptyMap(), error = null) }
        val monto = if (pideReembolso) estado.reembolso.trim().toInt() else null
        viewModelScope.launch {
            when (val resultado = viajes.cancelar(folio, estado.motivo.trim(), monto)) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, cancelado = true) }
                is Resultado.Fallo -> {
                    fallo(resultado.error)
                    // Un 409 casi siempre es que algo cambió: se vuelve a leer, conservando lo escrito.
                    if (resultado.error is ErrorApp.Conflicto) recargar(conservar = true)
                }
                // Cancelar no se encola: no hay conexión que esperar.
                Resultado.Encolado -> _estado.update { it.copy(enviando = false, error = ErrorApp.Inesperado) }
            }
        }
    }

    // Un 400 deja el motivo bajo su campo; lo que no es de un campo del formulario se muestra aparte.
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

    // Sin pago, o si no se puede leer, el viaje se cancela igual: el Administrador ve el aviso del error.
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
