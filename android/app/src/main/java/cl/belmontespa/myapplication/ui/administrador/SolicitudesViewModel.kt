package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.asignable
import cl.belmontespa.myapplication.model.retraso
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

/** Filtros de «Solicitudes» en el orden de la pantalla; el inicial es «Pendientes». */
enum class FiltroSolicitudes(val estado: EstadoViaje?) {
    PENDIENTES(EstadoViaje.PENDIENTE),
    ASIGNADO(EstadoViaje.ASIGNADO),
    EN_CURSO(EstadoViaje.EN_CURSO),
    FINALIZADO(EstadoViaje.FINALIZADO),
    CANCELADO(EstadoViaje.CANCELADO),
    TODAS(null),
}

/** Un viaje con lo que depende de "ahora": su retraso y si se le ofrece «Asignar» directo. */
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

@HiltViewModel
class SolicitudesViewModel @Inject constructor(
    private val viajes: ViajeRepository,
    private val cuentas: CuentaRepository,
    private val clock: Clock,
    private val guardado: SavedStateHandle,
) : ViewModel() {

    private val _estado = MutableStateFlow(SolicitudesUiState(filtro = filtroGuardado()))
    val estado: StateFlow<SolicitudesUiState> = _estado.asStateFlow()

    private var observacion: Job? = null

    // Los viajes tal como llegaron: al volver a primer plano se recalcula el retraso sobre ellos.
    private var ultimos: List<Viaje> = emptyList()

    init {
        actualizar()
    }

    fun elegirFiltro(filtro: FiltroSolicitudes) {
        if (filtro == _estado.value.filtro) return
        // Solo el nombre del filtro sobrevive a la muerte del proceso (README, Estado).
        guardado[CLAVE_FILTRO] = filtro.name
        ultimos = emptyList()
        _estado.update { it.copy(filtro = filtro, filas = emptyList(), cargando = true, error = null) }
        observar(filtro)
        viewModelScope.launch { avisarSiFalla(viajes.refrescarViajes(FiltroViajes(estado = filtro.estado))) }
    }

    /**
     * Al abrir, «Actualizar» y «Reintentar»: vuelve a leer y a pedir los viajes del filtro y las cuentas
     * por aprobar. Leer de nuevo también recupera una lista cuya lectura había fallado.
     */
    fun actualizar() {
        _estado.update { it.copy(error = null) }
        observar(_estado.value.filtro)
        viewModelScope.launch {
            avisarSiFalla(viajes.refrescarViajes(FiltroViajes(estado = _estado.value.filtro.estado)))
            actualizarCuentasPendientes()
        }
    }

    /** El retraso se calcula con el reloj: al volver a primer plano puede haber cambiado. */
    fun alVolverAPrimerPlano() {
        _estado.update { it.copy(filas = filas(ultimos)) }
        // HU27: al volver de revisar cuentas, conserva el filtro y actualiza su cantidad.
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
            // «Asignar» directo solo en las pendientes; reasignar es del detalle (HU23).
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
