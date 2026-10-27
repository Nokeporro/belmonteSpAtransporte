package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.ui.DATOS_INVALIDOS
import cl.belmontespa.myapplication.ui.FalloPantalla
import cl.belmontespa.myapplication.ui.capturar
import cl.belmontespa.myapplication.ui.valor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabilitacionesUiState(
    val conductores: List<Conductor> = emptyList(),
    val moviles: List<Movil> = emptyList(),
    val habilitaciones: List<Habilitacion> = emptyList(),
    /** Filtros de la lista, por patente y por correo del conductor; vacío es «todos». Independientes del alta. */
    val filtroVehiculo: String = "",
    val filtroConductor: String = "",
    /** Lo elegido para habilitar. */
    val vehiculo: String = "",
    val conductor: String = "",
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    /** Hay una selección sin guardar; los filtros no cuentan. */
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
)

/**
 * «Gestionar habilitaciones» (HU18): qué conductor puede manejar qué vehículo. Se abre desde un conductor o desde un
 * vehículo con ese lado ya elegido y filtrado; solo agrega relaciones entre activos y nunca las duplica.
 */
@HiltViewModel
class HabilitacionesViewModel @Inject constructor(
    private val flota: FlotaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(HabilitacionesUiState())
    val estado: StateFlow<HabilitacionesUiState> = _estado.asStateFlow()

    private var origen: String? = null
    private var consulta: Job? = null

    /**
     * [referencia] es `vehiculo:{patente}`, `conductor:{correo}` o vacía. Solo la primera lectura la aplica a los
     * filtros y a la selección; [forzar] vuelve a leer las listas sin perder lo que el Administrador ya eligió.
     */
    fun cargar(referencia: String, forzar: Boolean = false) {
        if (referencia == origen && !forzar) return
        val primera = origen == null
        origen = referencia
        consulta?.cancel()
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            if (primera) {
                val vehiculo = referencia.removePrefix("vehiculo:").takeIf { referencia.startsWith("vehiculo:") }.orEmpty()
                val conductor = referencia.removePrefix("conductor:").takeIf { referencia.startsWith("conductor:") }.orEmpty()
                _estado.update {
                    it.copy(filtroVehiculo = vehiculo, filtroConductor = conductor, vehiculo = vehiculo, conductor = conductor)
                }
            }
            var conductores = emptyList<Conductor>()
            var moviles = emptyList<Movil>()
            var habilitaciones = emptyList<Habilitacion>()
            val error = capturar(ErrorApp.Inesperado) {
                conductores = flota.listarConductores().valor()
                moviles = flota.listarMoviles().valor()
                habilitaciones = leerHabilitaciones()
            }
            _estado.update {
                if (error != null) it.copy(cargando = false, error = error)
                else it.copy(conductores = conductores, moviles = moviles, habilitaciones = habilitaciones, cargando = false)
            }
        }
    }

    private suspend fun leerHabilitaciones(): List<Habilitacion> = flota.listarHabilitaciones(
        patente = _estado.value.filtroVehiculo.ifBlank { null },
        correo = _estado.value.filtroConductor.ifBlank { null },
    ).valor()

    fun reintentar() {
        origen?.let { cargar(it, forzar = true) }
    }

    /** Cambiar un filtro vuelve a leer solo la lista; no toca la selección de alta ni la marca de cambios. */
    fun filtrarPorVehiculo(patente: String) {
        _estado.update { it.copy(filtroVehiculo = patente) }
        releer()
    }

    fun filtrarPorConductor(correo: String) {
        _estado.update { it.copy(filtroConductor = correo) }
        releer()
    }

    private fun releer() {
        viewModelScope.launch {
            var lista = emptyList<Habilitacion>()
            val error = capturar(ErrorApp.Inesperado) { lista = leerHabilitaciones() }
            _estado.update { if (error != null) it.copy(error = error) else it.copy(habilitaciones = lista, error = null) }
        }
    }

    fun elegirVehiculo(patente: String) =
        _estado.update { it.copy(vehiculo = patente, modificado = true, error = null, aviso = null) }

    fun elegirConductor(correo: String) =
        _estado.update { it.copy(conductor = correo, modificado = true, error = null, aviso = null) }

    fun habilitar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val errores = buildMap {
                    if (datos.moviles.none { it.patente == datos.vehiculo && it.activo }) put("vehiculo", "Selecciona un vehículo activo.")
                    if (datos.conductores.none { it.correo == datos.conductor && it.activo }) put("conductor", "Selecciona un conductor activo.")
                }
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                flota.habilitar(datos.vehiculo, datos.conductor).valor()
                val lista = leerHabilitaciones()
                _estado.update { it.copy(habilitaciones = lista, aviso = R.string.habilitacion_guardada, modificado = false) }
            }
            _estado.update { it.copy(enviando = false, error = error) }
        }
    }
}
