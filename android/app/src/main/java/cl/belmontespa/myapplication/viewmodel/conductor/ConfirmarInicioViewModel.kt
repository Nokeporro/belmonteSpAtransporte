package cl.belmontespa.myapplication.viewmodel.conductor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.arqueo
import cl.belmontespa.myapplication.model.disponibleDesde
import cl.belmontespa.myapplication.model.ventanaDeInicio
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfirmarInicioUiState(
    val cargando: Boolean = true,
    val viaje: Viaje? = null,
    /** Pasajeros que aún no abordaron: el aviso no impide iniciar (README, Detalle del Conductor). */
    val faltan: Int = 0,
    val inicio: Inicio = Inicio.NO_APLICA,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    /** Verdadero cuando el viaje quedó En curso: la pantalla vuelve al detalle. */
    val iniciado: Boolean = false,
) {
    val puedeIniciar: Boolean get() = inicio == Inicio.HABILITADO && !enviando && !iniciado

    /** «Disponible desde las {hora}»: solo antes de la ventana. */
    val disponibleDesde: LocalDateTime? get() = viaje?.takeIf { inicio == Inicio.ANTES_DE_LA_VENTANA }?.disponibleDesde()
}

/**
 * Confirmación de «Iniciar viaje» (HU09, V3). Avisa cuántos pasajeros faltan por abordar y, al confirmar, pasa el
 * viaje a `EN_CURSO`. El viaje es un `Flow` de Room: si algo cambia mientras la pantalla está abierta (otro
 * dispositivo, el reloj, el servidor) la habilitación del botón se recalcula sola.
 */
class ConfirmarInicioViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(ConfirmarInicioUiState())
    val estado: StateFlow<ConfirmarInicioUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var observacion: Job? = null

    /** Idempotente: la pantalla lo llama en cada composición con el mismo folio. */
    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        _estado.value = ConfirmarInicioUiState()
        iniciar()
    }

    /** «Reintentar»: vuelve a leer el viaje y a pedirlo. */
    fun actualizar() {
        _estado.update { it.copy(error = null) }
        iniciar()
    }

    /** La ventana de inicio depende del reloj: se recalcula al volver a primer plano. */
    fun alVolverAPrimerPlano() {
        _estado.update { actual -> actual.viaje?.let { actual.derivado(it) } ?: actual }
    }

    fun confirmar() {
        val folio = folio ?: return
        if (!_estado.value.puedeIniciar) return
        // `enviando` se marca antes de lanzar nada: un segundo toque no encuentra el botón libre.
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = viajes.cambiarEstado(folio, EstadoViaje.EN_CURSO)) {
                // Un cambio se muestra confirmado solo con la respuesta del servidor (README, Comunes).
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, iniciado = true) }
                is Resultado.Fallo -> {
                    _estado.update { it.copy(enviando = false, error = resultado.error) }
                    // Un 409 casi siempre es que algo cambió: se vuelve a pedir el viaje.
                    if (resultado.error is ErrorApp.Conflicto) refrescar()
                }
                // Sin conexión, Iniciar queda en cola (HU11 lo mostrará «Pendiente de sincronizar»).
                Resultado.Encolado -> _estado.update { it.copy(enviando = false) }
            }
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
                            it.copy(cargando = false, viaje = null, faltan = 0, inicio = Inicio.NO_APLICA,
                                error = it.error ?: ErrorApp.NoEncontrado)
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

    private fun ConfirmarInicioUiState.derivado(viaje: Viaje): ConfirmarInicioUiState = copy(
        viaje = viaje,
        faltan = viaje.arqueo()?.faltan?.size ?: 0,
        inicio = viaje.ventanaDeInicio(clock),
        // Si el viaje volvió a aparecer, el «no encontrado» anterior ya no vale.
        error = error.takeUnless { it == ErrorApp.NoEncontrado },
    )
}
