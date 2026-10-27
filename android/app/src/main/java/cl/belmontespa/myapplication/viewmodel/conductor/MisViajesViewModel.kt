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

/**
 * Un viaje de la agenda con lo que depende de "ahora": su retraso y si todavía falta para poder
 * iniciarlo (`fhAgenda − 1 h`), que decide a qué variante del detalle se entra.
 */
data class ViajeFila(
    val viaje: Viaje,
    val retraso: Retraso?,
    val antesDeLaVentana: Boolean,
)

data class MisViajesUiState(
    /** El día que se muestra. */
    val fecha: LocalDate,
    /** El día de hoy según el reloj: cambia si la app sigue abierta pasada la medianoche. */
    val hoy: LocalDate,
    /** Nombre del Conductor para el subtítulo; null mientras no llega (o si no se pudo leer). */
    val conductor: String? = null,
    val filas: List<ViajeFila> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
) {
    val esHoy: Boolean get() = fecha == hoy
}

/**
 * «Mis viajes» del Conductor (HU07): su agenda del día por hora, con selector de fecha. La lista es un
 * `Flow` de Room, así que una asignación, un inicio o una cancelación aparecen sin recargar.
 */
class MisViajesViewModel @JvmOverloads constructor(
    application: Application,
    private val guardado: SavedStateHandle,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val cuentas: CuentaRepository = CuentaRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    // null es «Hoy» y sigue al reloj; solo una fecha explícita distinta de hoy se recuerda.
    private var fechaElegida: LocalDate? = fechaGuardada()

    private val _estado = MutableStateFlow(
        LocalDate.now(clock).let { hoy -> MisViajesUiState(fecha = fechaElegida ?: hoy, hoy = hoy) },
    )
    val estado: StateFlow<MisViajesUiState> = _estado.asStateFlow()

    private var observacion: Job? = null

    // Los viajes tal como llegaron: al volver a primer plano se recalcula el retraso sobre ellos.
    private var ultimos: List<Viaje> = emptyList()

    init {
        actualizar()
        cargarNombre()
    }

    fun elegirFecha(fecha: LocalDate) {
        val nueva = fecha.takeUnless { it == _estado.value.hoy }
        if (nueva == fechaElegida) return
        fechaElegida = nueva
        // Solo la fecha explícita sobrevive a la muerte del proceso (README, Estado).
        if (nueva == null) guardado.remove<String>(CLAVE_FECHA) else guardado[CLAVE_FECHA] = nueva.toString()
        empezarDeNuevo()
    }

    /** Al abrir, «Actualizar» y «Reintentar»: vuelve a leer y a pedir la agenda del día. */
    fun actualizar() {
        _estado.update { it.copy(error = null) }
        observar()
        viewModelScope.launch { avisarSiFalla(viajes.refrescarViajes(filtro())) }
    }

    /**
     * El retraso depende del reloj y «hoy» también: si pasó la medianoche, la agenda de «Hoy» es otra
     * (y la consulta del `EN_CURSO` de un día anterior también).
     */
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
                    ultimos = lista
                    _estado.update { it.copy(filas = filas(lista), cargando = false) }
                }
        }
    }

    private fun filas(lista: List<Viaje>): List<ViajeFila> {
        val ahora = LocalDateTime.now(clock)
        return lista.map { viaje ->
            ViajeFila(
                viaje = viaje,
                retraso = viaje.retraso(clock),
                antesDeLaVentana = viaje.estado == EstadoViaje.ASIGNADO && ahora < viaje.disponibleDesde(),
            )
        }
    }

    // El nombre solo adorna el subtítulo: si no se puede leer, no es un error de la pantalla.
    private fun cargarNombre() {
        viewModelScope.launch {
            val perfil = cuentas.obtenerPerfil()
            if (perfil is Resultado.Exito) _estado.update { it.copy(conductor = perfil.valor.nombre) }
        }
    }

    private fun avisarSiFalla(resultado: Resultado<*>) {
        if (resultado is Resultado.Fallo) {
            _estado.update { it.copy(error = resultado.error, cargando = false) }
        }
    }

    private fun fechaGuardada(): LocalDate? =
        guardado.get<String>(CLAVE_FECHA)?.let { texto -> runCatching { LocalDate.parse(texto) }.getOrNull() }

    private companion object {
        const val CLAVE_FECHA = "fecha"
    }
}
