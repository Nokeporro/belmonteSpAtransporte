package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.asignable
import cl.belmontespa.myapplication.model.retraso
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FiltroSolicitudes(val estado: EstadoViaje?) {
    PENDIENTES(EstadoViaje.PENDIENTE),
    ASIGNADO(EstadoViaje.ASIGNADO),
    EN_CURSO(EstadoViaje.EN_CURSO),
    FINALIZADO(EstadoViaje.FINALIZADO),
    CANCELADO(EstadoViaje.CANCELADO),
    TODAS(null),
}

data class SolicitudFila(
    val viaje: Viaje,
    val retraso: Retraso?,
    val puedeAsignar: Boolean,
)

data class SolicitudesUiState(
    val filtro: FiltroSolicitudes = FiltroSolicitudes.PENDIENTES,
    val filas: List<SolicitudFila> = emptyList(),
    val cuentasPorAprobar: Int = 0,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

class SolicitudesViewModel @JvmOverloads constructor(
    application: Application,
    private val guardado: SavedStateHandle,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val cuentas: CuentaRepository = CuentaRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(SolicitudesUiState(filtro = filtroGuardado()))
    val estado: StateFlow<SolicitudesUiState> = _estado.asStateFlow()

    private var observacion: Job? = null

    private var ultimos: List<Viaje> = emptyList()

    init {
        actualizar()
    }

    fun elegirFiltro(filtro: FiltroSolicitudes) {
        if (filtro == _estado.value.filtro) return
        guardado[CLAVE_FILTRO] = filtro.name
        ultimos = emptyList()
        _estado.update { it.copy(filtro = filtro, filas = emptyList(), cargando = true, error = null) }
        observar(filtro)
        viewModelScope.launch { avisarSiFalla(viajes.refrescarViajes(FiltroViajes(estado = filtro.estado))) }
    }

    fun actualizar() {
        _estado.update { it.copy(error = null) }
        observar(_estado.value.filtro)
        viewModelScope.launch {
            avisarSiFalla(viajes.refrescarViajes(FiltroViajes(estado = _estado.value.filtro.estado)))
            actualizarCuentasPendientes()
        }
    }

    fun alVolverAPrimerPlano() {
        _estado.update { it.copy(filas = filas(ultimos)) }
        viewModelScope.launch { actualizarCuentasPendientes() }
    }

    private suspend fun actualizarCuentasPendientes() {
        when (val resultado = cuentas.listarSolicitantesPendientes()) {
            is Resultado.Exito -> _estado.update { it.copy(cuentasPorAprobar = resultado.valor.size) }
            else -> avisarSiFalla(resultado)
        }
    }

    private fun observar(filtro: FiltroSolicitudes) {
        observacion?.cancel()
        observacion = viewModelScope.launch {
            viajes.observarViajes(FiltroViajes(estado = filtro.estado))
                .catch { _estado.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
                .collect { lista ->
                    ultimos = lista
                    _estado.update { it.copy(filas = filas(lista), cargando = false) }
                }
        }
    }

    private fun filas(lista: List<Viaje>): List<SolicitudFila> = lista.map { viaje ->
        SolicitudFila(
            viaje = viaje,
            retraso = viaje.retraso(clock),
            puedeAsignar = viaje.estado == EstadoViaje.PENDIENTE && viaje.asignable(clock),
        )
    }

    private fun avisarSiFalla(resultado: Resultado<*>) {
        if (resultado is Resultado.Fallo) {
            _estado.update { it.copy(error = resultado.error, cargando = false) }
        }
    }

    private fun filtroGuardado(): FiltroSolicitudes =
        guardado.get<String>(CLAVE_FILTRO)
            ?.let { nombre -> FiltroSolicitudes.entries.firstOrNull { it.name == nombre } }
            ?: FiltroSolicitudes.PENDIENTES

    private companion object {
        const val CLAVE_FILTRO = "filtro"
    }
}
