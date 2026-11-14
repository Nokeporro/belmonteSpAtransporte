package cl.belmontespa.myapplication.viewmodel.conductor

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
import cl.belmontespa.myapplication.model.disponibleDesde
import cl.belmontespa.myapplication.model.retraso
import cl.belmontespa.myapplication.model.vigente
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViajeFila(
    val viaje: Viaje,
    val retraso: Retraso?,
    val antesDeLaVentana: Boolean,
)

data class MisViajesUiState(
    val fecha: LocalDate,
    val hoy: LocalDate,
    val conductor: String? = null,
    val filas: List<ViajeFila> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
    val sinConexion: Boolean = false,
) {
    val esHoy: Boolean get() = fecha == hoy
    val bannerSinConexion: Boolean get() = sinConexion && filas.any { it.viaje.cambioPendiente?.enConflicto == false }
}

class MisViajesViewModel @JvmOverloads constructor(
    application: Application,
    private val guardado: SavedStateHandle,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val cuentas: CuentaRepository = CuentaRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private var fechaElegida: LocalDate? = fechaGuardada()

    private val _estado = MutableStateFlow(
        LocalDate.now(clock).let { hoy -> MisViajesUiState(fecha = fechaElegida ?: hoy, hoy = hoy) },
    )
    val estado: StateFlow<MisViajesUiState> = _estado.asStateFlow()

    private var observacion: Job? = null

    private var ultimos: List<Viaje> = emptyList()

    init {
        actualizar()
        cargarNombre()
    }

    fun elegirFecha(fecha: LocalDate) {
        val nueva = fecha.takeUnless { it == _estado.value.hoy }
        if (nueva == fechaElegida) return
        fechaElegida = nueva
        if (nueva == null) guardado.remove<String>(CLAVE_FECHA) else guardado[CLAVE_FECHA] = nueva.toString()
        empezarDeNuevo()
    }

    fun actualizar() {
        _estado.update { it.copy(error = null) }
        observar()
        viewModelScope.launch { avisarSiFalla(viajes.refrescarViajes(filtro())) }
    }

    fun alVolverAPrimerPlano() {
        val hoy = LocalDate.now(clock)
        if (hoy != _estado.value.hoy) {
            _estado.update { it.copy(hoy = hoy) }
            empezarDeNuevo()
        } else {
            _estado.update { it.copy(filas = filas(ultimos)) }
        }
    }

    private fun empezarDeNuevo() {
        ultimos = emptyList()
        _estado.update {
            it.copy(fecha = fechaElegida ?: it.hoy, filas = emptyList(), cargando = true, error = null)
        }
        observar()
        viewModelScope.launch { avisarSiFalla(viajes.refrescarViajes(filtro())) }
    }

    private fun filtro() = FiltroViajes(fecha = _estado.value.fecha)

    private fun observar() {
        observacion?.cancel()
        observacion = viewModelScope.launch {
            viajes.observarViajes(filtro())
                .catch { _estado.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
                .collect { lista ->
                    val sincronizado = ultimos.any { it.cambioPendiente?.enConflicto == false } && lista.none { it.cambioPendiente?.enConflicto == false }
                    ultimos = lista
                    _estado.update {
                        val base = if (sincronizado && it.error == ErrorApp.SinConexion) it.copy(error = null, sinConexion = false) else it
                        base.copy(filas = filas(lista), cargando = false)
                    }
                }
        }
    }

    private fun filas(lista: List<Viaje>): List<ViajeFila> {
        val ahora = LocalDateTime.now(clock)
        return lista.map { viaje ->
            val vigente = viaje.vigente()
            ViajeFila(
                viaje = viaje,
                retraso = vigente.retraso(clock),
                antesDeLaVentana = vigente.estado == EstadoViaje.ASIGNADO && ahora < vigente.disponibleDesde(),
            )
        }
    }

    private fun cargarNombre() {
        viewModelScope.launch {
            val perfil = cuentas.obtenerPerfil()
            if (perfil is Resultado.Exito) _estado.update { it.copy(conductor = perfil.valor.nombre) }
        }
    }

    private fun avisarSiFalla(resultado: Resultado<*>) {
        if (resultado is Resultado.Fallo) {
            _estado.update { it.copy(error = resultado.error, cargando = false, sinConexion = resultado.error == ErrorApp.SinConexion) }
        } else {
            _estado.update { it.copy(sinConexion = false) }
        }
    }

    private fun fechaGuardada(): LocalDate? =
        guardado.get<String>(CLAVE_FECHA)?.let { texto -> runCatching { LocalDate.parse(texto) }.getOrNull() }

    private companion object {
        const val CLAVE_FECHA = "fecha"
    }
}
