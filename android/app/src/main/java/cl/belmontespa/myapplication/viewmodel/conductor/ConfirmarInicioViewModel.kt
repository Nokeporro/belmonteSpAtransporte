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
    val faltan: Int = 0,
    val inicio: Inicio = Inicio.NO_APLICA,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val iniciado: Boolean = false,
) {
    val puedeIniciar: Boolean get() = inicio == Inicio.HABILITADO && !enviando && !iniciado

    val disponibleDesde: LocalDateTime? get() = viaje?.takeIf { inicio == Inicio.ANTES_DE_LA_VENTANA }?.disponibleDesde()
}

class ConfirmarInicioViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(ConfirmarInicioUiState())
    val estado: StateFlow<ConfirmarInicioUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var observacion: Job? = null

    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        _estado.value = ConfirmarInicioUiState()
        iniciar()
    }

    fun actualizar() {
        _estado.update { it.copy(error = null) }
        iniciar()
    }

    fun alVolverAPrimerPlano() {
        _estado.update { actual -> actual.viaje?.let { actual.derivado(it) } ?: actual }
    }

    fun confirmar() {
        val folio = folio ?: return
        if (!_estado.value.puedeIniciar) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = viajes.cambiarEstado(folio, EstadoViaje.EN_CURSO)) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, iniciado = true) }
                is Resultado.Fallo -> {
                    _estado.update { it.copy(enviando = false, error = resultado.error) }
                    if (resultado.error is ErrorApp.Conflicto) refrescar()
                }
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
            if (resultado is Resultado.Fallo) _estado.update { it.copy(cargando = false, error = resultado.error) }
        }
    }

    private fun ConfirmarInicioUiState.derivado(viaje: Viaje): ConfirmarInicioUiState = copy(
        viaje = viaje,
        faltan = viaje.arqueo()?.faltan?.size ?: 0,
        inicio = viaje.ventanaDeInicio(clock),
        error = error.takeUnless { it == ErrorApp.NoEncontrado },
    )
}
