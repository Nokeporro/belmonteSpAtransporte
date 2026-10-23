package cl.belmontespa.myapplication.ui.conductor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.Arqueo
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.arqueo
import cl.belmontespa.myapplication.model.disponibleDesde
import cl.belmontespa.myapplication.model.enVentanaDeInicio
import cl.belmontespa.myapplication.model.retraso
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Dónde está «Iniciar viaje» respecto de la ventana de V3 (`fhAgenda` ± 1 h). */
enum class Inicio { HABILITADO, ANTES_DE_LA_VENTANA, VENCIDO, NO_APLICA }

data class DetalleViajeUiState(
    val cargando: Boolean = true,
    val viaje: Viaje? = null,
    val retraso: Retraso? = null,
    val arqueo: Arqueo? = null,
    val inicio: Inicio = Inicio.NO_APLICA,
    val confirmandoFin: Boolean = false,
    val rechazando: Boolean = false,
    val motivo: String = "",
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    /** Verdadero cuando el rechazo quedó confirmado: el viaje ya no es suyo y la pantalla vuelve a «Mis viajes». */
    val rechazado: Boolean = false,
) {
    private val estado: EstadoViaje? get() = viaje?.estado

    /** Los botones de estado solo existen mientras el viaje está `ASIGNADO` o `EN_CURSO`. */
    val conControles: Boolean get() = estado == EstadoViaje.ASIGNADO || estado == EstadoViaje.EN_CURSO
    val puedeIniciar: Boolean get() = inicio == Inicio.HABILITADO && !enviando
    val puedeFinalizar: Boolean get() = estado == EstadoViaje.EN_CURSO && !enviando
    val puedeRechazar: Boolean get() = estado == EstadoViaje.ASIGNADO && !enviando
    val puedeConfirmarRechazo: Boolean get() = puedeRechazar && motivo.isNotBlank()

    /** «Disponible desde las {hora}»: solo antes de la ventana. */
    val disponibleDesde: LocalDateTime? get() = viaje?.takeIf { inicio == Inicio.ANTES_DE_LA_VENTANA }?.disponibleDesde()
}

/**
 * Detalle del viaje del Conductor (HU08): datos, arqueo de abordaje y los controles de estado con su
 * regla de visibilidad y habilitación (README, «Detalle del Conductor»). El viaje es un `Flow` de Room,
 * así que el arqueo y el estado se actualizan sin recargar.
 */
@HiltViewModel
class DetalleViajeViewModel @Inject constructor(
    private val viajes: ViajeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleViajeUiState())
    val estado: StateFlow<DetalleViajeUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var observacion: Job? = null

    /** Idempotente: la pantalla lo llama en cada composición con el mismo folio. */
    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        _estado.value = DetalleViajeUiState()
        iniciar()
    }

    /** «Actualizar» y «Reintentar»: vuelve a leer el viaje y a pedirlo. */
    fun actualizar() {
        _estado.update { it.copy(error = null) }
        iniciar()
    }

    /** La ventana de inicio y el retraso dependen del reloj: se recalculan al volver a primer plano. */
    fun alVolverAPrimerPlano() {
        _estado.update { actual -> actual.viaje?.let { actual.derivado(it) } ?: actual }
    }

    fun pedirFinalizar() {
        if (_estado.value.puedeFinalizar) _estado.update { it.copy(confirmandoFin = true) }
    }

    fun descartarFinalizar() {
        _estado.update { it.copy(confirmandoFin = false) }
    }

    fun confirmarFinalizar() {
        val folio = folio ?: return
        if (!_estado.value.puedeFinalizar) return
        // `enviando` se marca antes de lanzar nada: un segundo toque no encuentra el botón libre.
        _estado.update { it.copy(enviando = true, confirmandoFin = false, error = null) }
        viewModelScope.launch { terminarEnvio(viajes.cambiarEstado(folio, EstadoViaje.FINALIZADO)) }
    }

    fun pedirRechazo() {
        if (_estado.value.puedeRechazar) _estado.update { it.copy(rechazando = true) }
    }

    fun escribirMotivo(texto: String) {
        _estado.update { it.copy(motivo = texto) }
    }

    /** Cierra el diálogo y conserva lo escrito: al reabrirlo, el motivo sigue ahí. */
    fun descartarRechazo() {
        _estado.update { it.copy(rechazando = false) }
    }

    fun confirmarRechazo() {
        val folio = folio ?: return
        val actual = _estado.value
        if (!actual.puedeConfirmarRechazo) return
        _estado.update { it.copy(enviando = true, rechazando = false, error = null) }
        viewModelScope.launch {
            val resultado = viajes.rechazar(folio, actual.motivo.trim())
            if (resultado is Resultado.Exito) _estado.update { it.copy(motivo = "", rechazado = true) }
            terminarEnvio(resultado)
        }
    }

    // Un cambio se muestra confirmado solo con la respuesta del servidor (README, Comunes).
    private fun terminarEnvio(resultado: Resultado<Unit>) {
        when (resultado) {
            is Resultado.Exito -> _estado.update { it.copy(enviando = false) }
            is Resultado.Fallo -> {
                _estado.update { it.copy(enviando = false, error = resultado.error) }
                // Un 409 casi siempre es que algo cambió: se vuelve a pedir el viaje.
                if (resultado.error is ErrorApp.Conflicto) refrescar()
            }
            // Sin conexión, Iniciar y Finalizar quedan en cola (HU11 los mostrará «Pendiente de sincronizar»).
            Resultado.Encolado -> _estado.update { it.copy(enviando = false) }
        }
    }

    private fun iniciar() {
        val folio = folio ?: return
        observacion?.cancel()
        observacion = viewModelScope.launch {
            viajes.observarViaje(folio)
                .catch { _estado.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
                .collect { viaje ->
                    if (viaje == null) {
                        // Un viaje ajeno, reasignado o que no existe se ve igual (README, 403 y 404).
                        _estado.update {
                            it.copy(cargando = false, viaje = null, retraso = null, arqueo = null,
                                inicio = Inicio.NO_APLICA, error = it.error ?: ErrorApp.NoEncontrado)
                        }
                    } else {
                        _estado.update { it.copy(cargando = false).derivado(viaje) }
                    }
                }
        }
        refrescar()
    }

    private fun refrescar() {
        val folio = folio ?: return
        viewModelScope.launch {
            val resultado = viajes.refrescarViaje(folio)
            // Un fallo del refresco se avisa, pero no borra lo ya cargado.
            if (resultado is Resultado.Fallo) _estado.update { it.copy(cargando = false, error = resultado.error) }
        }
    }

    private fun DetalleViajeUiState.derivado(viaje: Viaje): DetalleViajeUiState {
        val ahora = LocalDateTime.now(clock)
        val inicio = when {
            viaje.estado != EstadoViaje.ASIGNADO -> Inicio.NO_APLICA
            viaje.enVentanaDeInicio(clock) -> Inicio.HABILITADO
            ahora < viaje.disponibleDesde() -> Inicio.ANTES_DE_LA_VENTANA
            else -> Inicio.VENCIDO
        }
        return copy(
            viaje = viaje,
            retraso = viaje.retraso(clock),
            arqueo = viaje.arqueo(),
            inicio = inicio,
            // Si el viaje volvió a aparecer, el «no encontrado» anterior ya no vale.
            error = error.takeUnless { it == ErrorApp.NoEncontrado },
        )
    }
}
